package xin.vanilla.banira.internal.forge.compat.jei;

import com.mojang.blaze3d.matrix.MatrixStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.config.IWorldConfig;
import mezz.jei.gui.overlay.bookmarks.BookmarkButton;
import mezz.jei.input.click.MouseClickState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.ModList;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonAccessor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Forge 1.16.5 JEI 书签按钮兼容桥。 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static volatile BookmarkButton bookmarkButton;
    private static volatile IDrawable bookmarkOffIcon;
    private static volatile IDrawable bookmarkOnIcon;
    private static volatile IWorldConfig bookmarkWorldConfig;

    private JeiCompatibility() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("jei")) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
    }

    public static void capture(Object value, IDrawable offIcon, IDrawable onIcon,
                               IWorldConfig worldConfig) {
        if (!(value instanceof BookmarkButton)) return;
        bookmarkButton = (BookmarkButton) value;
        bookmarkOffIcon = offIcon;
        bookmarkOnIcon = onIcon;
        bookmarkWorldConfig = worldConfig;
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
                    screen, 0, 0, 0, MouseClickState.EXECUTE);
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void drawBookmarkIcon(MatrixStack stack, int x, int y, int size) {
        IWorldConfig worldConfig = bookmarkWorldConfig;
        IDrawable icon = worldConfig != null && worldConfig.isBookmarkOverlayEnabled()
                ? bookmarkOnIcon : bookmarkOffIcon;
        if (icon == null || size <= 0) return;

        int sourceWidth = Math.max(1, icon.getWidth());
        int sourceHeight = Math.max(1, icon.getHeight());
        float scale = Math.min(size / (float) sourceWidth, size / (float) sourceHeight);
        float offsetX = (size - sourceWidth * scale) / 2f;
        float offsetY = (size - sourceHeight * scale) / 2f;
        stack.pushPose();
        try {
            stack.translate(x + offsetX, y + offsetY, 0);
            stack.scale(scale, scale, 1f);
            icon.draw(stack, 0, 0);
        } finally {
            stack.popPose();
        }
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
