package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;

import javax.annotation.Nonnull;

/**
 * 屏幕绘制事件；子 mod 应通过 draw 调用稳定绘制能力。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraDrawScreenEvent {
    private final @Nonnull BaniraDrawContext draw;
    private final @Nonnull BaniraScreenInfo screen;
    private final double mouseX;
    private final double mouseY;
    private final float partialTick;

    public BaniraDrawScreenEvent(@Nonnull BaniraDrawContext draw, @Nonnull BaniraScreenInfo screen,
                                 double mouseX, double mouseY, float partialTick) {
        this.draw = draw;
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTick = partialTick;
    }
}
