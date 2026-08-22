package xin.vanilla.banira.internal.fabric.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.gui.overlay.bookmarks.BookmarkButton;
import mezz.jei.gui.input.InputType;
import mezz.jei.gui.input.UserInput;
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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Fabric 1.20.1 JEI 书签按钮兼容桥。 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static volatile BookmarkButton bookmarkButton;
    private static volatile IDrawable bookmarkOffIcon;
    private static volatile IDrawable bookmarkOnIcon;
    private static volatile IClientToggleState bookmarkToggleState;

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

    public static boolean isManagedBookmark(Object value) {
        return value == bookmarkButton || value != null
                && "mezz.jei.gui.overlay.bookmarks.BookmarkButton".equals(
                value.getClass().getName());
    }

    public static boolean shouldSuppress(Object value) {
        return isManagedBookmark(value)
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

    private static void drawBookmarkIcon(PoseStack stack, int x, int y, int size) {
        IClientToggleState toggleState = bookmarkToggleState;
        IDrawable icon = toggleState != null && toggleState.isBookmarkOverlayEnabled()
                ? bookmarkOnIcon : bookmarkOffIcon;
        if (icon == null || size <= 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        icon.draw(graphics, x + Math.max(0, (size - icon.getWidth()) / 2),
                y + Math.max(0, (size - icon.getHeight()) / 2));
    }

    private static final class BookmarkProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            return Collections.singletonList(new ExternalInventoryAction(
                    "bookmarks",
                    BaniraComponent.get().literal(I18n.get("jei.tooltip.bookmarks")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawBookmarkIcon(stack, x, y, size)),
                    context -> activate()));
        }
    }
}
