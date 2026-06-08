package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderContext;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;

import javax.annotation.Nonnull;

/**
 * HUD overlay 绘制事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraOverlayRenderEvent {
    private final @Nonnull HudOverlayElement element;
    private final @Nonnull BaniraHudRenderContext context;
    private final float partialTick;
    private final boolean screenOpen;

    public BaniraOverlayRenderEvent(@Nonnull HudOverlayElement element, @Nonnull BaniraHudRenderContext context,
                                    float partialTick, boolean screenOpen) {
        this.element = element;
        this.context = context;
        this.partialTick = partialTick;
        this.screenOpen = screenOpen;
    }
}
