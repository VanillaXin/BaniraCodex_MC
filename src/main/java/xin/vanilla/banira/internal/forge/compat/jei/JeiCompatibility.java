package xin.vanilla.banira.internal.forge.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.core.config.IWorldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.fml.ModList;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryAction;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryActionProvider;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * JEI 9/10 bookmark button bridge.
 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static volatile Object bookmarkButton;
    private static volatile Object publishedBookmark;
    private static volatile Method clickMethod;
    private static volatile Object clickInput;
    private static volatile IDrawable bookmarkOffIcon;
    private static volatile IDrawable bookmarkOnIcon;
    private static volatile IWorldConfig bookmarkWorldConfig;

    private JeiCompatibility() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("jei")) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
    }

    public static void capture(Object value) {
        bookmarkButton = null;
        publishedBookmark = null;
        try {
            IDrawable off = (IDrawable) field(value, "offIcon");
            IDrawable on = (IDrawable) field(value, "onIcon");
            IWorldConfig config = (IWorldConfig) field(value, "worldConfig");
            Method click = null;
            for (Method method : value.getClass().getDeclaredMethods()) {
                if (method.getName().equals("onMouseClicked") && method.getParameterCount() == 1
                        && method.getReturnType() == boolean.class) click = method;
            }
            if (click == null || off == null || on == null || config == null) return;
            Object input = mouseInput(click.getParameterTypes()[0]);
            if (input == null || !click.trySetAccessible()) return;
            bookmarkOffIcon = off;
            bookmarkOnIcon = on;
            bookmarkWorldConfig = config;
            clickMethod = click;
            clickInput = input;
            bookmarkButton = value;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            bookmarkButton = null;
        } finally {
            refreshHost();
        }
    }

    private static void refreshHost() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.screen != null)
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
    }

    private static void releaseBookmark() {
        bookmarkButton = null;
        publishedBookmark = null;
        refreshHost();
    }

    private static Object field(Object value, String name) throws ReflectiveOperationException {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                if (!field.trySetAccessible()) throw new IllegalAccessException(name);
                return field.get(value);
            } catch (NoSuchFieldException next) {
                // JEI stores icons in the superclass and configuration on the button.
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Object mouseInput(Class<?> type) throws ReflectiveOperationException {
        for (Method factory : type.getMethods()) {
            Class<?>[] args = factory.getParameterTypes();
            if (!factory.getName().equals("fromVanilla") || !java.lang.reflect.Modifier.isStatic(factory.getModifiers())
                    || args.length < 3 || args[0] != double.class || args[1] != double.class || args[2] != int.class)
                continue;
            Object input;
            if (args.length == 3) input = factory.invoke(null, 0.0, 0.0, 0);
            else if (args.length == 4 && args[3].isEnum())
                input = factory.invoke(null, 0.0, 0.0, 0, args[3].getField("EXECUTE").get(null));
            else continue;
            if (input instanceof Optional<?>) input = ((Optional<?>) input).orElse(null);
            if (type.isInstance(input) && Boolean.FALSE.equals(type.getMethod("isSimulate").invoke(input)))
                return input;
        }
        return null;
    }

    public static boolean isManagedBookmark(Object value) {
        return value != null && value == bookmarkButton;
    }

    public static boolean shouldSuppress(Object value) {
        return JeiBookmarkContract.ready() && isManagedBookmark(value) && value == publishedBookmark
                && ExternalInventoryButtonManager.get().suppressesNativeButtons(SOURCE_ID);
    }

    public static boolean shouldBlockNativeClick(Object value) {
        return shouldSuppress(value) && !FORWARDING_CLICK.get();
    }

    private static void activate() {
        Object button = bookmarkButton;
        Screen screen = Minecraft.getInstance().screen;
        if (button == null || screen == null) return;
        FORWARDING_CLICK.set(true);
        try {
            clickMethod.invoke(button, clickInput);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            releaseBookmark();
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void drawBookmarkIcon(PoseStack stack, int x, int y, int size) {
        if (size <= 0) return;
        try {
            drawCapturedBookmarkIcon(stack, x, y, size);
        } catch (RuntimeException | LinkageError unavailable) {
            releaseBookmark();
        }
    }

    private static void drawCapturedBookmarkIcon(PoseStack stack, int x, int y, int size) {
        IWorldConfig worldConfig = bookmarkWorldConfig;
        IDrawable icon = worldConfig != null && worldConfig.isBookmarkOverlayEnabled()
                ? bookmarkOnIcon : bookmarkOffIcon;
        if (icon == null) throw new IllegalStateException("Missing JEI bookmark icon");

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
            Object button = bookmarkButton;
            if (button == null || !JeiBookmarkContract.ready()) {
                publishedBookmark = null;
                return Collections.emptyList();
            }
            publishedBookmark = button;
            return Collections.singletonList(new ExternalInventoryAction(
                    "bookmarks",
                    BaniraComponent.get().literal(I18n.get("jei.tooltip.bookmarks")),
                    QuickIcon.custom((stack, minecraft, x, y, size) ->
                            drawBookmarkIcon(stack, x, y, size)),
                    context -> activate()));
        }
    }
}
