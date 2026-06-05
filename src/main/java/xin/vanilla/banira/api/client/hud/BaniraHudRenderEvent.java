package xin.vanilla.banira.api.client.hud;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * HUD 元素渲染事件；Pre 阶段可取消原版绘制。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraHudRenderEvent {
    private final @Nonnull HudRenderPhase phase;
    private final @Nonnull HudOverlayElement element;
    private final @Nonnull BaniraHudRenderContext context;
    private final @Nonnull BaniraHudBounds bounds;
    private final boolean cancellable;
    private boolean canceled;

    public BaniraHudRenderEvent(@Nonnull HudRenderPhase phase, @Nonnull HudOverlayElement element,
                                @Nonnull BaniraHudRenderContext context, boolean cancellable) {
        this(phase, element, context, BaniraHudBounds.empty(), cancellable);
    }

    public BaniraHudRenderEvent(@Nonnull HudRenderPhase phase, @Nonnull HudOverlayElement element,
                                @Nonnull BaniraHudRenderContext context, @Nonnull BaniraHudBounds bounds,
                                boolean cancellable) {
        this.phase = phase;
        this.element = element;
        this.context = context;
        this.bounds = bounds;
        this.cancellable = cancellable;
    }

    public void cancel() {
        if (cancellable) {
            canceled = true;
        }
    }

    public boolean isPre() {
        return phase == HudRenderPhase.PRE;
    }

    public boolean isPost() {
        return phase == HudRenderPhase.POST;
    }

    public boolean isExperience() {
        return element.isExperience();
    }

    public boolean hasKnownBounds() {
        return bounds.isKnown();
    }
}
