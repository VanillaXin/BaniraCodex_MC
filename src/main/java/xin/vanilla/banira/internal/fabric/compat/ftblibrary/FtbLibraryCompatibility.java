package xin.vanilla.banira.internal.fabric.compat.ftblibrary;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButton;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonGroup;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager;
import dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContextMenuItem;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Fabric 1.20.1 FTB Library 侧边栏与 Banira 快捷入口之间的双向桥。 */
public final class FtbLibraryCompatibility implements ExternalInventoryButtonManager.FtbHostBridge {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation GROUP_ID =
            new ResourceLocation("banira_codex", "external_inventory_buttons");
    private static final String BUTTON_PATH_PREFIX = "external_inventory_buttons/";
    private static final FtbLibraryCompatibility INSTANCE = new FtbLibraryCompatibility();
    private static final Map<ResourceLocation, ExternalInventoryAction> HOSTED_ACTIONS =
            new LinkedHashMap<>();
    private static Field iconField;

    private FtbLibraryCompatibility() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("ftblibrary")) return;
        ExternalInventoryButtonManager manager = ExternalInventoryButtonManager.get();
        manager.registerProvider(new FtbButtonProvider());
        manager.setFtbHostBridge(INSTANCE);
    }

    @Override
    public boolean available() {
        return FabricLoader.getInstance().isModLoaded("ftblibrary");
    }

    @Override
    public synchronized void replace(@Nullable Screen screen, List<ExternalInventoryAction> actions) {
        clear();
        if (actions == null || actions.isEmpty()) return;

        SidebarButtonGroup group = new SidebarButtonGroup(GROUP_ID, 300);
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
            HOSTED_ACTIONS.put(id, action);
        }
        if (!group.getButtons().isEmpty()) {
            SidebarButtonManager.INSTANCE.getGroups().add(group);
            Collections.sort(SidebarButtonManager.INSTANCE.getGroups());
        }
    }

    @Override
    public synchronized void clear() {
        SidebarButtonManager.INSTANCE.getGroups().removeIf(group -> GROUP_ID.equals(group.getId()));
        HOSTED_ACTIONS.clear();
    }

    public static boolean shouldSuppressNativeGroup() {
        return ExternalInventoryButtonManager.get().suppressesNativeButtons(
                ExternalInventoryButtonManager.FTB_SOURCE_ID);
    }

    public static void updateGroupWidgetVisibility(Object value) {
        if (value instanceof SidebarGroupGuiButton) {
            ((SidebarGroupGuiButton) value).visible = !shouldSuppressNativeGroup();
        }
    }

    /** 清除 FTB 留给侧边按钮的区域，使 JEI 收藏区重新使用完整高度。 */
    public static void clearReservedArea() {
        SidebarGroupGuiButton.lastDrawnArea = new Rect2i(0, 0, 0, 0);
    }

    /** 返回 true 表示该按钮属于 Banira 动态组，调用方应取消 FTB 原处理。 */
    public static boolean activateHostedButton(Object value, boolean shiftDown) {
        if (!(value instanceof SidebarButton)) return false;
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
        if (!(value instanceof SidebarButton)) return null;
        return HOSTED_ACTIONS.containsKey(((SidebarButton) value).getId())
                ? "sidebar_button.banira_codex.external_inventory_button" : null;
    }

    private static void setIcon(SidebarButton button, Icon icon) {
        try {
            if (iconField == null) {
                iconField = SidebarButton.class.getDeclaredField("icon");
                iconField.setAccessible(true);
            }
            iconField.set(button, icon);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Unable to preserve a Banira quick-action icon in FTB Library", exception);
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
