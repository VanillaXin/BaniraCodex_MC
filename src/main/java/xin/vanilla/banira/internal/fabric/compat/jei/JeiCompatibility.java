package xin.vanilla.banira.internal.fabric.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.config.IClientToggleState;
import mezz.jei.gui.input.InputType;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.overlay.bookmarks.BookmarkButton;
import mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButton;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
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

/**
 * 将 JEI 的书签与查询历史开关接入统一快捷入口。
 */
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

    public static void captureCreated(Object value, boolean historyAction) {
        if (failed || value == null) return;
        try {
            if (historyAction) {
                captureLookupHistory(value, (IDrawable) nativeField(value, "offIcon"), (IDrawable) nativeField(value, "onIcon"), (IClientConfig) nativeField(value, "clientConfig"));
            } else {
                capture(value, (IDrawable) nativeField(value, "offIcon"), (IDrawable) nativeField(value, "onIcon"), (IClientToggleState) nativeField(value, "toggleState"));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
    }

    private static Object nativeField(Object value, String name) throws ReflectiveOperationException {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(value);
            } catch (NoSuchFieldException next) {
                // Native toggle icons may be declared on a superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static volatile boolean failed;

    private static void disableCompatibility(Throwable failure) {
        if (failed) return;
        failed = true;
        org.apache.logging.log4j.LogManager.getLogger().warn("JEI shortcut integration disabled; native buttons retained", failure);
    }

    private JeiCompatibility() {
    }

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("jei")) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
    }

    public static void capture(Object value, IDrawable offIcon, IDrawable onIcon,
                               IClientToggleState toggleState) {
        if (failed) return;
        try {
            if (!(value instanceof BookmarkButton)) return;
            bookmarkButton = (BookmarkButton) value;
            bookmarkOffIcon = offIcon;
            bookmarkOnIcon = onIcon;
            bookmarkToggleState = toggleState;

        } catch (RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
    }

    public static void captureLookupHistory(Object value, IDrawable offIcon,
                                            IDrawable onIcon, IClientConfig clientConfig) {
        if (failed) return;
        try {
            if (!(value instanceof LookupHistoryButton)) return;
            lookupHistoryButton = (LookupHistoryButton) value;
            lookupHistoryOffIcon = offIcon;
            lookupHistoryOnIcon = onIcon;
            lookupHistoryConfig = clientConfig;

        } catch (RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
    }

    public static boolean isManagedBookmark(Object value) {
        return value != null && value == bookmarkButton;
    }

    public static boolean isManagedLookupHistory(Object value) {
        return value != null && value == lookupHistoryButton;
    }

    public static boolean shouldSuppress(Object value) {
        if (failed) return false;
        return (isManagedBookmark(value) || isManagedLookupHistory(value))
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
    }

    public static boolean shouldBlockNativeClick(Object value) {
        return shouldSuppress(value) && !FORWARDING_CLICK.get();
    }

    private static void activate() {
        if (failed) return;
        try {
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

        } catch (RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
    }

    private static void activateLookupHistory() {
        if (failed) return;
        try {
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

        } catch (RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
    }

    private static void drawBookmarkIcon(PoseStack stack, int x, int y, int size) {
        if (failed) return;
        try {
            IClientToggleState toggleState = bookmarkToggleState;
            IDrawable icon = toggleState != null && toggleState.isBookmarkOverlayEnabled()
                    ? bookmarkOnIcon : bookmarkOffIcon;
            drawNativeIcon(stack, x, y, size, icon);

        } catch (RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
    }

    private static void drawLookupHistoryIcon(PoseStack stack, int x, int y, int size) {
        if (failed) return;
        try {
            IClientConfig config = lookupHistoryConfig;
            IDrawable icon = config != null && config.isLookupHistoryEnabled()
                    ? lookupHistoryOnIcon : lookupHistoryOffIcon;
            drawNativeIcon(stack, x, y, size, icon);

        } catch (RuntimeException | LinkageError unavailable) {
            disableCompatibility(unavailable);
        }
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
        try {
            graphics.pose().translate(x + (size - width) / 2.0F,
                    y + (size - height) / 2.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            icon.draw(graphics, 0, 0);

        } finally {
            graphics.pose().popPose();
        }
    }

    private static final class BookmarkProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            if (failed) return java.util.Collections.emptyList();
            List<ExternalInventoryAction> actions = new ArrayList<>();
            if (bookmarkButton != null) actions.add(new ExternalInventoryAction(
                    "bookmarks",
                    BaniraComponent.get().literal(I18n.get("jei.tooltip.bookmarks")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawBookmarkIcon(stack, x, y, size)),
                    context -> activate()));
            if (lookupHistoryButton != null) actions.add(new ExternalInventoryAction(
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
