package xin.vanilla.banira.internal.fabric.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.gui.overlay.bookmarks.BookmarkButton;
import mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButton;
import mezz.jei.gui.input.InputType;
import mezz.jei.gui.input.UserInput;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.config.IClientToggleState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.fabricmc.loader.api.FabricLoader;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonAccessor;
import xin.vanilla.banira.internal.mixin.compat.jei.LookupHistoryButtonAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** 将 JEI 的书签与查询历史开关接入统一快捷入口。 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static volatile BookmarkButton bookmarkButton;
    private static volatile IDrawable bookmarkOffIcon;
    private static volatile IDrawable bookmarkOnIcon;
    private static volatile IClientToggleState bookmarkToggleState;
    private static volatile LookupHistoryButton lookupHistoryButton;
    private static volatile IDrawable lookupHistoryOffIcon;
    private static volatile IDrawable lookupHistoryOnIcon;
    private static volatile IClientConfig lookupHistoryConfig;

    private JeiCompatibility() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
    }

    public static void capture(Object value, IDrawable offIcon, IDrawable onIcon,
                               IClientToggleState toggleState) {
        if (!(value instanceof BookmarkButton)) return;
        bookmarkButton = (BookmarkButton) value;
        bookmarkOffIcon = offIcon;
        bookmarkOnIcon = onIcon;
        bookmarkToggleState = toggleState;
    }

    public static void captureLookupHistory(Object value, IDrawable offIcon,
                                            IDrawable onIcon, IClientConfig clientConfig) {
        if (!(value instanceof LookupHistoryButton)) return;
        lookupHistoryButton = (LookupHistoryButton) value;
        lookupHistoryOffIcon = offIcon;
        lookupHistoryOnIcon = onIcon;
        lookupHistoryConfig = clientConfig;
    }

    public static boolean isManagedBookmark(Object value) {
        return value == bookmarkButton || value != null
                && "mezz.jei.gui.overlay.bookmarks.BookmarkButton".equals(
                value.getClass().getName());
    }

    public static boolean isManagedLookupHistory(Object value) {
        return value == lookupHistoryButton || value != null
                && "mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButton".equals(
                value.getClass().getName());
    }

    public static boolean shouldSuppress(Object value) {
        return (isManagedBookmark(value) || isManagedLookupHistory(value))
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
    }

    public static boolean shouldBlockNativeClick(Object value) {
        return shouldSuppress(value) && !FORWARDING_CLICK.get();
    }

    private static void activate() {
        BookmarkButton button = bookmarkButton;
        Screen screen = Minecraft.getInstance().screen;
        if (button == null || screen == null) return;
        FORWARDING_CLICK.set(true);
        try {
            ((BookmarkButtonAccessor) (Object) button).banira$invokeMouseClicked(
                    UserInput.fromVanilla(0, 0, 0, InputType.EXECUTE));
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void activateLookupHistory() {
        LookupHistoryButton button = lookupHistoryButton;
        Screen screen = Minecraft.getInstance().screen;
        if (button == null || screen == null) return;
        FORWARDING_CLICK.set(true);
        try {
            ((LookupHistoryButtonAccessor) (Object) button).banira$invokeMouseClicked(
                    UserInput.fromVanilla(0, 0, 0, InputType.EXECUTE));
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void drawBookmarkIcon(PoseStack stack, int x, int y, int size) {
        IClientToggleState toggleState = bookmarkToggleState;
        IDrawable icon = toggleState != null && toggleState.isBookmarkOverlayEnabled()
                ? bookmarkOnIcon : bookmarkOffIcon;
        drawNativeIcon(stack, x, y, size, icon);
    }

    private static void drawLookupHistoryIcon(PoseStack stack, int x, int y, int size) {
        IClientConfig config = lookupHistoryConfig;
        IDrawable icon = config != null && config.isLookupHistoryEnabled()
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
            actions.add(new ExternalInventoryAction(
                    "bookmarks",
                    BaniraComponent.get().literal(I18n.get("jei.tooltip.bookmarks")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawBookmarkIcon(stack, x, y, size)),
                    context -> activate()));
            actions.add(new ExternalInventoryAction(
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
