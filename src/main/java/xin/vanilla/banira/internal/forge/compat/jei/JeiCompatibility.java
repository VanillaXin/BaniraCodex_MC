package xin.vanilla.banira.internal.forge.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.config.IClientToggleState;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import xin.vanilla.banira.internal.client.compat.jei.JeiRuntimeAccess;
import mezz.jei.gui.overlay.bookmarks.BookmarkButtonController;
import mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButtonController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.fml.ModList;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 JEI 的书签与查询历史控制器接入统一快捷入口。
 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static volatile boolean registered;
    private static volatile BookmarkButtonController bookmarkController;
    private static volatile IDrawable bookmarkOffIcon;
    private static volatile IDrawable bookmarkOnIcon;
    private static volatile IClientToggleState bookmarkToggleState;
    private static volatile LookupHistoryButtonController lookupHistoryController;
    private static volatile IDrawable lookupHistoryOffIcon;
    private static volatile IDrawable lookupHistoryOnIcon;
    private static volatile IClientConfig lookupHistoryConfig;

    private JeiCompatibility() {
    }

    public static void init() {
        if (registered || !ModList.get().isLoaded("jei")) return;
        if (!JeiRuntimeAccess.supportsConfig(IClientConfig.class)
                || !JeiRuntimeAccess.supportsInput(JeiCompatibility.class.getClassLoader())) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
        registered = true;
    }

    public static void capture(Object value, IDrawable offIcon, IDrawable onIcon,
                               IClientToggleState toggleState) {
        if (!(value instanceof BookmarkButtonController)) return;
        bookmarkController = (BookmarkButtonController) value;
        bookmarkOffIcon = offIcon;
        bookmarkOnIcon = onIcon;
        bookmarkToggleState = toggleState;
        if (registered) ExternalInventoryButtonManager.get().refreshCurrentScreen();
    }

    public static void captureLookupHistory(Object value, IDrawable offIcon,
                                            IDrawable onIcon, IClientConfig clientConfig) {
        if (!(value instanceof LookupHistoryButtonController)) return;
        lookupHistoryController = (LookupHistoryButtonController) value;
        lookupHistoryOffIcon = offIcon;
        lookupHistoryOnIcon = onIcon;
        lookupHistoryConfig = clientConfig;
        if (registered) ExternalInventoryButtonManager.get().refreshCurrentScreen();
    }

    public static boolean isManagedBookmark(Object value) {
        return value != null && value == bookmarkController;
    }

    public static boolean isManagedLookupHistory(Object value) {
        return value != null && value == lookupHistoryController;
    }

    public static boolean shouldSuppress(Object value) {
        return registered && (isManagedBookmark(value) || isManagedLookupHistory(value))
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
    }

    public static boolean shouldBlockNativeClick(Object value) {
        return shouldSuppress(value) && !FORWARDING_CLICK.get();
    }

    private static void activateBookmark() {
        BookmarkButtonController controller = bookmarkController;
        Screen screen = Minecraft.getInstance().screen;
        if (controller == null || screen == null) return;
        Object input = JeiRuntimeAccess.mouseInput(JeiCompatibility.class.getClassLoader());
        if (input instanceof IJeiUserInput) forwardClick(() -> controller.onPress((IJeiUserInput) input));
    }

    private static void activateLookupHistory() {
        LookupHistoryButtonController controller = lookupHistoryController;
        Screen screen = Minecraft.getInstance().screen;
        if (controller == null || screen == null) return;
        Object input = JeiRuntimeAccess.mouseInput(JeiCompatibility.class.getClassLoader());
        if (input instanceof IJeiUserInput) forwardClick(() -> controller.onPress((IJeiUserInput) input));
    }

    private static void forwardClick(Runnable action) {
        FORWARDING_CLICK.set(true);
        try {
            action.run();
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void drawBookmarkIcon(PoseStack stack, int x, int y, int size) {
        IClientToggleState state = bookmarkToggleState;
        IDrawable icon = state != null && state.isBookmarkOverlayEnabled()
                ? bookmarkOnIcon : bookmarkOffIcon;
        drawNativeIcon(stack, x, y, size, icon);
    }

    private static void drawLookupHistoryIcon(PoseStack stack, int x, int y, int size) {
        IClientConfig config = lookupHistoryConfig;
        IDrawable icon = JeiRuntimeAccess.lookupHistoryEnabled(config)
                ? lookupHistoryOnIcon : lookupHistoryOffIcon;
        drawNativeIcon(stack, x, y, size, icon);
    }

    private static void drawNativeIcon(PoseStack stack, int x, int y, int size,
                                       IDrawable icon) {
        if (icon == null || size <= 0 || icon.getWidth() <= 0 || icon.getHeight() <= 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        graphics.pose().last().pose().set(stack.last().pose());
        float scale = Math.min((float) size / icon.getWidth(),
                (float) size / icon.getHeight());
        float width = icon.getWidth() * scale;
        float height = icon.getHeight() * scale;
        graphics.pose().pushPose();
        graphics.pose().translate(x + (size - width) / 2.0F,
                y + (size - height) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        icon.draw(graphics, 0, 0);
        graphics.pose().popPose();
    }

    private static final class BookmarkProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            List<ExternalInventoryAction> actions = new ArrayList<>();
            if (bookmarkController != null) actions.add(new ExternalInventoryAction(
                    "bookmarks",
                    BaniraComponent.get().literal(I18n.get("word.banira_codex.jei_bookmarks")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawBookmarkIcon(stack, x, y, size)),
                    context -> activateBookmark()));
            if (lookupHistoryController != null) actions.add(new ExternalInventoryAction(
                    "lookup_history",
                    BaniraComponent.get().literal(I18n.get(
                            "word.banira_codex.jei_lookup_history")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawLookupHistoryIcon(stack, x, y, size)),
                    context -> activateLookupHistory()));
            return actions;
        }
    }
}
