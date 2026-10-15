package xin.vanilla.banira.internal.forge.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;
import xin.vanilla.banira.internal.mixin.compat.jei.BookmarkButtonAccessor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Forge 1.19.2 JEI 书签按钮兼容桥。
 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static volatile Object bookmarkButton;
    private static volatile IDrawable bookmarkOffIcon;
    private static volatile IDrawable bookmarkOnIcon;
    private static volatile Runnable bookmarkClick;

    private JeiCompatibility() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("jei")) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
    }

    public static void capture(Object value, IDrawable offIcon, IDrawable onIcon) {
        bookmarkButton = null;
        try {
            bookmarkClick = JeiBookmarkClick.bind(value);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Unsupported JEI bookmark input contract; retaining native button", exception);
            return;
        }
        bookmarkOffIcon = offIcon;
        bookmarkOnIcon = onIcon;
        bookmarkButton = value;
    }

    public static boolean isManagedBookmark(Object value) {
        return value != null && value == bookmarkButton;
    }

    public static boolean shouldSuppress(Object value) {
        return isManagedBookmark(value)
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
    }

    public static boolean shouldBlockNativeClick(Object value) {
        return shouldSuppress(value) && !FORWARDING_CLICK.get();
    }

    private static void activate() {
        Runnable click = bookmarkClick;
        Screen screen = Minecraft.getInstance().screen;
        if (bookmarkButton == null || click == null || screen == null) return;
        FORWARDING_CLICK.set(true);
        try {
            click.run();
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void drawBookmarkIcon(PoseStack stack, int x, int y, int size) {
        Object button = bookmarkButton;
        IDrawable icon = button != null && ((BookmarkButtonAccessor) button).banira$isIconToggledOn()
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
            if (bookmarkButton == null) return Collections.emptyList();
            return Collections.singletonList(new ExternalInventoryAction(
                    "bookmarks",
                    BaniraComponent.get().literal(I18n.get("jei.tooltip.bookmarks")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawBookmarkIcon(stack, x, y, size)),
                    context -> activate()));
        }
    }
}
