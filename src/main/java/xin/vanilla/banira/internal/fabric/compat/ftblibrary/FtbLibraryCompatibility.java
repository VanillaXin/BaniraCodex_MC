package xin.vanilla.banira.internal.fabric.compat.ftblibrary;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.sidebar.GridLocation;
import dev.ftb.mods.ftblibrary.sidebar.RegisteredSidebarButton;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonData;
import dev.ftb.mods.ftblibrary.sidebar.SidebarButtonManager;
import dev.ftb.mods.ftblibrary.sidebar.SidebarGuiButton;
import dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Optional;
import java.util.function.Consumer;

/** Fabric 1.21.1 FTB Library 侧边栏与 Banira 快捷入口之间的双向桥。 */
public final class FtbLibraryCompatibility implements ExternalInventoryButtonManager.FtbHostBridge {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BUTTON_PATH_PREFIX = "external_inventory_buttons/";
    private static final FtbLibraryCompatibility INSTANCE = new FtbLibraryCompatibility();
    private static final Map<ResourceLocation, ExternalInventoryAction> HOSTED_ACTIONS = new LinkedHashMap<>();
    private static Field buttonsField;

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

        int order = 0;
        for (ExternalInventoryAction action : actions) {
            if (action == null || action.onActivate() == null && action.contextMenuItems().isEmpty()) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("banira_codex",
                    BUTTON_PATH_PREFIX + safePath(action.sourceId() + "/" + action.id()));
            SidebarButtonData data = new SidebarButtonData(
                    new QuickActionFtbIcon(action.icon()), true, Collections.emptyList(), Optional.empty(),
                    false, Optional.of(Collections.singletonList(action.label().toVanilla())), Optional.empty(),
                    false, Optional.empty(), 300 + order);
            RegisteredSidebarButton button = new RegisteredSidebarButton(id, data);
            button.setTooltipOverride(() -> Collections.singletonList(action.label().toVanilla()));
            register(button, new SidebarGuiButton(new GridLocation(order++, 0), true, button));
            HOSTED_ACTIONS.put(id, action);
        }
    }

    @Override
    public synchronized void clear() {
        SidebarButtonManager.INSTANCE.getButtonList().removeIf(button ->
                HOSTED_ACTIONS.containsKey(button.getSidebarButton().getId()));
        try {
            buttonMap().keySet().removeAll(HOSTED_ACTIONS.keySet());
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Unable to remove Banira quick actions from FTB Library", exception);
        }
        HOSTED_ACTIONS.clear();
    }

    public static boolean shouldSuppressNativeGroup() {
        return ExternalInventoryButtonManager.get().suppressesNativeButtons(
                ExternalInventoryButtonManager.FTB_SOURCE_ID);
    }

    /** 清除 FTB 留给侧边按钮的区域，使其他覆盖层重新使用完整高度。 */
    public static void clearReservedArea() {
        SidebarGroupGuiButton.lastDrawnArea = new Rect2i(0, 0, 0, 0);
    }

    public static boolean activateHostedButton(Object value, boolean shiftDown) {
        if (!(value instanceof RegisteredSidebarButton)) return false;
        ExternalInventoryAction action = HOSTED_ACTIONS.get(((RegisteredSidebarButton) value).getId());
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
        if (!(value instanceof RegisteredSidebarButton)) return null;
        return HOSTED_ACTIONS.containsKey(((RegisteredSidebarButton) value).getId())
                ? "sidebar_button.banira_codex.external_inventory_button" : null;
    }

    private static void register(RegisteredSidebarButton button, SidebarGuiButton guiButton) {
        try {
            buttonMap().put(button.getId(), button);
            SidebarButtonManager.INSTANCE.getButtonList().add(guiButton);
        } catch (ReflectiveOperationException exception) {
            LOGGER.warn("Unable to expose a Banira quick action through FTB Library", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, RegisteredSidebarButton> buttonMap() throws ReflectiveOperationException {
        if (buttonsField == null) {
            buttonsField = SidebarButtonManager.class.getDeclaredField("buttons");
            buttonsField.setAccessible(true);
        }
        return (Map<ResourceLocation, RegisteredSidebarButton>) buttonsField.get(SidebarButtonManager.INSTANCE);
    }

    private static String safePath(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    private static final class FtbButtonProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return ExternalInventoryButtonManager.FTB_SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            List<ExternalInventoryAction> actions = new ArrayList<>();
            for (RegisteredSidebarButton button : SidebarButtonManager.INSTANCE.getButtons()) {
                if (HOSTED_ACTIONS.containsKey(button.getId()) || !button.canSee()) continue;
                List<Component> tooltip = button.getTooltip(false);
                String label = tooltip.isEmpty() ? button.getId().toString() : tooltip.get(0).getString();
                actions.add(new ExternalInventoryAction(button.getId().toString(),
                        BaniraComponent.get().literal(label),
                        QuickIcon.custom((stack, minecraft, x, y, size) ->
                                button.getData().icon().draw(guiGraphics(stack), x, y, size, size)),
                        context -> button.clickButton(Screen.hasShiftDown())));
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
