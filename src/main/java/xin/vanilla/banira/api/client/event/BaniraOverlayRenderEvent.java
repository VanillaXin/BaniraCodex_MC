package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;

import javax.annotation.Nonnull;

/**
 * HUD overlay 绘制事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraOverlayRenderEvent {
    private final @Nonnull HudOverlayElement element;
    private final @Nonnull Object nativeGraphics;
    private final float partialTick;
    private final boolean screenOpen;
    private final @Nonnull Object nativeEvent;

    public BaniraOverlayRenderEvent(@Nonnull HudOverlayElement element, @Nonnull Object nativeGraphics,
                                    float partialTick, boolean screenOpen, @Nonnull Object nativeEvent) {
        this.element = element;
        this.nativeGraphics = nativeGraphics;
        this.partialTick = partialTick;
        this.screenOpen = screenOpen;
        this.nativeEvent = nativeEvent;
    }
}
