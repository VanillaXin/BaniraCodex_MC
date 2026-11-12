package xin.vanilla.banira.internal.forge.compat.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forge 1.19.2 JEI 书签按钮兼容桥。
 */
public final class JeiCompatibility {
    public static final String SOURCE_ID = "jei";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadLocal<Boolean> FORWARDING_CLICK =
            ThreadLocal.withInitial(() -> false);
    private static final AtomicBoolean REFRESH_PENDING = new AtomicBoolean();
    private static volatile CapturedButton bookmark;
    private static volatile CapturedButton history;
    private static boolean initialized;

    private JeiCompatibility() {
    }

    public static synchronized void init() {
        if (initialized || !ModList.get().isLoaded("jei")) return;
        ExternalInventoryButtonManager.get().registerProvider(new BookmarkProvider());
        MinecraftForge.EVENT_BUS.addListener(JeiCompatibility::onClientTick);
        initialized = true;
        REFRESH_PENDING.set(true);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && REFRESH_PENDING.getAndSet(false)) {
            ExternalInventoryButtonManager.get().refreshCurrentScreen();
        }
    }

    public static synchronized void capture(Object value, IDrawable offIcon, IDrawable onIcon) {
        CapturedButton next = null;
        try {
            if (value != null && offIcon != null && onIcon != null) {
                next = new CapturedButton(value, offIcon, onIcon, JeiBookmarkClick.bind(value), null);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            LOGGER.warn("Unsupported JEI bookmark input contract; retaining native button", exception);
        } finally {
            bookmark = next;
            REFRESH_PENDING.set(true);
        }
    }

    public static boolean isManagedBookmark(Object value) {
        CapturedButton captured = bookmark;
        return captured != null && value == captured.button;
    }

    public static synchronized void captureHistory(Object value) {
        CapturedButton next = null;
        try {
            if (value != null) {
                IDrawable off = (IDrawable) field(value, "offIcon");
                IDrawable on = (IDrawable) field(value, "onIcon");
                Method toggled = value.getClass().getDeclaredMethod("isIconToggledOn");
                if (off != null && on != null && toggled.trySetAccessible()) {
                    next = new CapturedButton(value, off, on, JeiBookmarkClick.bind(value), toggled);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            LOGGER.warn("Unsupported JEI history button; retaining native button", unavailable);
        } finally {
            history = next;
            REFRESH_PENDING.set(true);
        }
    }

    private static Object field(Object value, String name) throws ReflectiveOperationException {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                if (!field.trySetAccessible()) throw new IllegalAccessException(name);
                return field.get(value);
            } catch (NoSuchFieldException next) {
                // Icons are inherited from the native toggle button.
            }
        }
        throw new NoSuchFieldException(name);
    }

    public static boolean shouldSuppress(Object value) {
        CapturedButton captured = bookmark;
        if (captured != null && value == captured.button && JeiButtonContract.ready(false)) return hosted(captured);
        captured = history;
        return captured != null && value == captured.button && JeiButtonContract.ready(true) && hosted(captured);
    }

    private static boolean hosted(CapturedButton captured) {
        return ExternalInventoryButtonManager.get().suppressesNativeAction(SOURCE_ID, captured.action);
    }

    public static boolean shouldBlockNativeClick(Object value) {
        return shouldSuppress(value) && !FORWARDING_CLICK.get();
    }

    private static boolean current(CapturedButton captured, boolean historyAction) {
        return captured == (historyAction ? history : bookmark);
    }

    private static void activate(CapturedButton captured, boolean historyAction) {
        if (!current(captured, historyAction) || !hosted(captured)
                || !JeiButtonContract.ready(historyAction) || Minecraft.getInstance().screen == null) return;
        FORWARDING_CLICK.set(true);
        try {
            captured.click.run();
        } catch (RuntimeException | LinkageError unavailable) {
            disable(captured, historyAction, unavailable);
        } finally {
            FORWARDING_CLICK.remove();
        }
    }

    private static void drawIcon(CapturedButton captured, boolean historyAction, PoseStack stack, int x, int y, int size) {
        if (!current(captured, historyAction)) return;
        try {
            boolean toggled = historyAction ? Boolean.TRUE.equals(captured.toggled.invoke(captured.button))
                    : ((BookmarkButtonAccessor) captured.button).banira$isIconToggledOn();
            drawIcon(stack, x, y, size, toggled ? captured.on : captured.off);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) {
            disable(captured, historyAction, unavailable);
        }
    }

    private static synchronized void disable(CapturedButton captured, boolean historyAction, Throwable failure) {
        if (!current(captured, historyAction)) return;
        if (historyAction) history = null;
        else bookmark = null;
        // Invalidate ownership now; rebuild the host outside constructors, rendering and input dispatch.
        REFRESH_PENDING.set(true);
        LOGGER.warn("JEI {} action disabled; native button restored", historyAction ? "history" : "bookmark", failure);
    }

    private static void drawIcon(PoseStack stack, int x, int y, int size, IDrawable icon) {
        if (size <= 0) return;
        int sourceWidth = icon.getWidth();
        int sourceHeight = icon.getHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) throw new IllegalStateException("Invalid JEI icon dimensions");
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

    private static final class CapturedButton {
        private final Object button;
        private final IDrawable off;
        private final IDrawable on;
        private final Runnable click;
        private final Method toggled;
        private volatile ExternalInventoryAction action;

        private CapturedButton(Object button, IDrawable off, IDrawable on, Runnable click, Method toggled) {
            this.button = button;
            this.off = off;
            this.on = on;
            this.click = click;
            this.toggled = toggled;
        }

        private synchronized ExternalInventoryAction action(boolean historyAction) {
            // Stable identity distinguishes the installed action from a replacement with the same id.
            if (action == null) {
                action = new ExternalInventoryAction(historyAction ? "lookup_history" : "bookmarks",
                        BaniraComponent.get().literal(I18n.get(historyAction
                                ? "word.banira_codex.jei_lookup_history" : "jei.tooltip.bookmarks")),
                        QuickIcon.custom((stack, minecraft, x, y, size) -> drawIcon(this, historyAction, stack, x, y, size)),
                        context -> activate(this, historyAction));
            }
            return action;
        }
    }

    private static final class BookmarkProvider implements ExternalInventoryActionProvider {
        @Override
        public String sourceId() {
            return SOURCE_ID;
        }

        @Override
        public List<ExternalInventoryAction> actions(@Nullable Screen screen) {
            List<ExternalInventoryAction> actions = new ArrayList<>();
            CapturedButton captured = bookmark;
            if (captured != null && JeiButtonContract.ready(false)) actions.add(captured.action(false));
            captured = history;
            if (captured != null && JeiButtonContract.ready(true)) actions.add(captured.action(true));
            return actions;
        }
    }
}
