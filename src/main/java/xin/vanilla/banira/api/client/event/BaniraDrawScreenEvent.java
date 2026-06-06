package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 屏幕绘制事件；nativeGraphics 在当前 Forge 1.18.2 分支为 PoseStack。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraDrawScreenEvent {
    private final @Nonnull Object nativeGraphics;
    private final @Nonnull Object screen;
    private final double mouseX;
    private final double mouseY;
    private final float partialTick;
    private final @Nonnull Object nativeEvent;

    public BaniraDrawScreenEvent(@Nonnull Object nativeGraphics, @Nonnull Object screen,
                                 double mouseX, double mouseY, float partialTick, @Nonnull Object nativeEvent) {
        this.nativeGraphics = nativeGraphics;
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTick = partialTick;
        this.nativeEvent = nativeEvent;
    }
}
