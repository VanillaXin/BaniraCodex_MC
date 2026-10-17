package xin.vanilla.banira.internal.fabric.compat.ftblibrary;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButton;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonGroup;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager;
import dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.*;
import xin.vanilla.banira.internal.compat.FtbLibraryContract;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Fabric 1.20.1 FTB Library 侧边栏与 Banira 快捷入口之间的双向桥。
 */
public final class FtbLibraryCompatibility implements ExternalInventoryButtonManager.FtbHostBridge {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation GROUP_ID =
            new ResourceLocation("banira_codex", "external_inventory_buttons");
    private static final String BUTTON_PATH_PREFIX = "external_inventory_buttons/";
    private static final FtbLibraryCompatibility INSTANCE = new FtbLibraryCompatibility();
    private static final Map<ResourceLocation, ExternalInventoryAction> HOSTED_ACTIONS =
            new LinkedHashMap<>();
    private static Field iconField;
    private static volatile boolean ready;

    private FtbLibraryCompatibility() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("ftblibrary")) return;
        if (ready) return;
        try {
            if (!FtbLibraryContract.matches()) throw new IllegalStateException(FtbLibraryContract.mismatch());
            ClassLoader loader = FtbLibraryCompatibility.class.getClassLoader();
            Class.forName("dev.ftb.mods.ftblibrary.sidebar.SidebarButton", false, loader);
            Class.forName("dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton", false, loader);
            if (!FtbLibraryContract.hooksApplied()) {
                throw new IllegalStateException("FTB sidebar render/click hooks were not fully applied");
            }
            iconField = FtbLibraryContract.bindIcon(SidebarButton.class, Icon.class);
            ready = true;
            ExternalInventoryButtonManager manager = ExternalInventoryButtonManager.get();
            manager.setFtbHostBridge(INSTANCE);
            manager.registerProvider(new FtbButtonProvider());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            ready = false;
            iconField = null;
            LOGGER.error("FTB Library compatibility disabled; native controls remain active", exception);
        }
    }

    @Override
    public boolean available() {
        return ready;
    }

    @Override
    public synchronized void replace(@Nullable Screen screen, List<ExternalInventoryAction> actions) {
        if (!ready) throw new IllegalStateException("FTB sidebar bridge is not ready");
        if (actions == null || actions.isEmpty()) {
            clear();
            return;
        }

        SidebarButtonGroup group = new SidebarButtonGroup(GROUP_ID, 300);
        Map<ResourceLocation, ExternalInventoryAction> staged = new LinkedHashMap<>();
        int order = 0;
        for (ExternalInventoryAction action : actions) {
            if (action == null || action.onActivate() == null && action.contextMenuItems().isEmpty()) continue;
            ResourceLocation id = new ResourceLocation("banira_codex",
                    BUTTON_PATH_PREFIX + safePath(action.sourceId() + "/" + action.id()));
            JsonObject json = new JsonObject();
            json.addProperty("x", order++);
            json.addProperty("icon", "minecraft:paper");
            SidebarButton button = new SidebarButton(id, group, json);
            setIcon(button, new QuickActionFtbIcon(action.icon()));
            button.setTooltipHandler(lines -> lines.add(action.label().toVanilla().getString()));
            group.getButtons().add(button);
            staged.put(id, action);
        }
        // Publish click ownership only after every native button has been prepared.
        clear();
        try {
            if (!group.getButtons().isEmpty()) {
                SidebarButtonManager.INSTANCE.getGroups().add(group);
                Collections.sort(SidebarButtonManager.INSTANCE.getGroups());
            }
            HOSTED_ACTIONS.putAll(staged);
        } catch (RuntimeException | LinkageError exception) {
            try {
                clear();
            } catch (RuntimeException | LinkageError cleanup) {
                exception.addSuppressed(cleanup);
            }
            throw exception;
        }
    }

    @Override
    public synchronized void clear() {
        HOSTED_ACTIONS.clear();
        if (ready) {
            SidebarButtonManager.INSTANCE.getGroups().removeIf(group -> GROUP_ID.equals(group.getId()));
        }
    }

    public static boolean shouldSuppressNativeGroup() {
        return ready && ExternalInventoryButtonManager.get().suppressesNativeButtons(
                ExternalInventoryButtonManager.FTB_SOURCE_ID);
    }

    public static void updateGroupWidgetVisibility(Object value) {
        if (!ready) return;
        if (value instanceof SidebarGroupGuiButton) {
            ((SidebarGroupGuiButton) value).visible = !shouldSuppressNativeGroup();
        }
    }

    /**
     * 清除 FTB 留给侧边按钮的区域，使 JEI 收藏区重新使用完整高度。
     */
    public static void clearReservedArea() {
        SidebarGroupGuiButton.lastDrawnArea = new Rect2i(0, 0, 0, 0);
    }

    /**
     * 返回 true 表示该按钮属于 Banira 动态组，调用方应取消 FTB 原处理。
     */
    public static boolean activateHostedButton(Object value, boolean shiftDown) {
        if (!ready || !(value instanceof SidebarButton)) return false;
        ExternalInventoryAction action = HOSTED_ACTIONS.get(((SidebarButton) value).getId());
        if (action == null) return false;

        Consumer<QuickActionContext> consumer = action.onActivate();
        if (shiftDown && !action.contextMenuItems().isEmpty()) {
            QuickActionContextMenuItem item = action.contextMenuItems().get(0);
            if (item != null && item.getOnActivate() != null) consumer = item.getOnActivate();
        } else if (consumer == null && !action.contextMenuItems().isEmpty()) {
            QuickActionContextMenuItem item = action.contextMenuItems().get(0);
            if (item != null) consumer = item.getOnActivate();
        }
        if (consumer != null) {
            consumer.accept(new QuickActionContext().currentScreen(Minecraft.getInstance().screen));
        }
        return true;
    }

    @Nullable
    public static String hostedButtonTranslationKey(Object value) {
        if (!ready || !(value instanceof SidebarButton)) return null;
        return HOSTED_ACTIONS.containsKey(((SidebarButton) value).getId())
                ? "sidebar_button.banira_codex.external_inventory_button" : null;
    }

    private static void setIcon(SidebarButton button, Icon icon) {
        try {
            if (!ready || iconField == null) throw new IllegalStateException("FTB icon binding is unavailable");
            iconField.set(button, icon);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot set the FTB sidebar action icon", exception);
        }
    }

    private static String safePath(String value) {
        return value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9/._-]", "_");
    }

    private static final class FtbButtonProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return ExternalInventoryButtonManager.FTB_SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            if (!ready) throw new IllegalStateException("FTB sidebar bridge is not ready");
            List<ExternalInventoryAction> actions = new ArrayList<>();
            for (SidebarButtonGroup group : SidebarButtonManager.INSTANCE.getGroups()) {
                if (GROUP_ID.equals(group.getId())) continue;
                for (SidebarButton button : group.getButtons()) {
                    if (button.isActuallyVisible()) {
                        actions.add(new ExternalInventoryAction(button.getId().toString(),
                                BaniraComponent.get().literal(I18n.get(button.getLangKey())),
                                QuickIcon.custom((stack, minecraft, x, y, size) ->
                                        button.getIcon().draw(guiGraphics(stack), x, y, size, size)),
                                context -> {
                                    boolean shiftDown = Screen.hasShiftDown();
                                    button.onClicked(shiftDown);
                                }));
                    }
                }
            }
            return actions;
        }
    }

    private static final class QuickActionFtbIcon extends Icon {
        private final QuickIcon icon;

        private QuickActionFtbIcon(QuickIcon icon) {
            this.icon = icon;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
            int size = Math.min(width, height);
            icon.render(graphics, Minecraft.getInstance(),
                    x + (width - size) / 2, y + (height - size) / 2, size);
        }
    }

    private static GuiGraphics guiGraphics(PoseStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.pose().last().pose().set(stack.last().pose());
        return graphics;
    }
}
