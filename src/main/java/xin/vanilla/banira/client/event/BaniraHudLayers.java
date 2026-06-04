package xin.vanilla.banira.client.event;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Convenience facade for replacing or extending Banira HUD elements without using loader events directly.
 */
public final class BaniraHudLayers {
    private static final List<Entry> beforeVanilla = new CopyOnWriteArrayList<>();
    private static final List<Entry> afterVanilla = new CopyOnWriteArrayList<>();

    static {
        BaniraClientEventHub.Hud.onPreRender(BaniraHudLayers::dispatchBeforeVanilla);
        BaniraClientEventHub.Hud.onPostRender(BaniraHudLayers::dispatchAfterVanilla);
    }

    private BaniraHudLayers() {
    }

    /**
     * Runs before vanilla draws the element. Call {@link BaniraHudRenderEvent#cancel()} to hide vanilla rendering.
     */
    public static Registration before(@Nonnull BaniraHudOverlayElement element, @Nonnull Consumer<BaniraHudRenderEvent> renderer) {
        return register(beforeVanilla, element, renderer);
    }

    /**
     * Intercepts an element before vanilla rendering; the callback may observe, cancel, or draw extra content.
     */
    public static Registration intercept(@Nonnull BaniraHudOverlayElement element, @Nonnull Consumer<BaniraHudRenderEvent> interceptor) {
        return before(element, interceptor);
    }

    /**
     * Runs after vanilla draws the element.
     */
    public static Registration after(@Nonnull BaniraHudOverlayElement element, @Nonnull Consumer<BaniraHudRenderEvent> renderer) {
        return register(afterVanilla, element, renderer);
    }

    /**
     * Cancels vanilla rendering for the element and lets the callback draw a replacement.
     */
    public static Registration replace(@Nonnull BaniraHudOverlayElement element, @Nonnull Consumer<BaniraHudRenderEvent> renderer) {
        return before(element, event -> {
            event.cancel();
            renderer.accept(event);
        });
    }

    public static Registration replaceExperienceBar(@Nonnull Consumer<BaniraHudRenderEvent> renderer) {
        return replace(BaniraHudOverlayElement.EXPERIENCE_BAR, renderer);
    }

    public static Registration interceptExperienceBar(@Nonnull Consumer<BaniraHudRenderEvent> interceptor) {
        return intercept(BaniraHudOverlayElement.EXPERIENCE_BAR, interceptor);
    }

    /**
     * Replaces the experience level text when the current loader/version exposes it separately.
     */
    public static Registration replaceExperienceText(@Nonnull Consumer<BaniraHudRenderEvent> renderer) {
        return replace(BaniraHudOverlayElement.EXPERIENCE_TEXT, renderer);
    }

    public static Registration interceptExperienceText(@Nonnull Consumer<BaniraHudRenderEvent> interceptor) {
        return intercept(BaniraHudOverlayElement.EXPERIENCE_TEXT, interceptor);
    }

    public static Registration hide(@Nonnull BaniraHudOverlayElement element) {
        return before(element, BaniraHudRenderEvent::cancel);
    }

    public static Registration hideExperienceBar() {
        return hide(BaniraHudOverlayElement.EXPERIENCE_BAR);
    }

    public static Registration hideExperienceText() {
        return hide(BaniraHudOverlayElement.EXPERIENCE_TEXT);
    }

    public interface Registration {
        void unregister();
    }

    private static Registration register(List<Entry> entries, BaniraHudOverlayElement element, Consumer<BaniraHudRenderEvent> renderer) {
        Entry entry = new Entry(element, renderer);
        entries.add(entry);
        return () -> entries.remove(entry);
    }

    private static void dispatchBeforeVanilla(BaniraHudRenderEvent event) {
        dispatch(beforeVanilla, event);
    }

    private static void dispatchAfterVanilla(BaniraHudRenderEvent event) {
        dispatch(afterVanilla, event);
    }

    private static void dispatch(List<Entry> entries, BaniraHudRenderEvent event) {
        for (Entry entry : entries) {
            if (entry.matches(event.element())) {
                entry.renderer.accept(event);
            }
        }
    }

    private static final class Entry {
        private final BaniraHudOverlayElement element;
        private final Consumer<BaniraHudRenderEvent> renderer;

        private Entry(BaniraHudOverlayElement element, Consumer<BaniraHudRenderEvent> renderer) {
            this.element = element != null ? element : BaniraHudOverlayElement.UNKNOWN;
            this.renderer = renderer != null ? renderer : event -> {
            };
        }

        private boolean matches(BaniraHudOverlayElement renderedElement) {
            return element == BaniraHudOverlayElement.ALL || element == renderedElement;
        }
    }
}
