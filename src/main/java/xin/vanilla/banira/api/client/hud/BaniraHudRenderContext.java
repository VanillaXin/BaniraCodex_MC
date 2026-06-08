package xin.vanilla.banira.api.client.hud;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;

import javax.annotation.Nonnull;

/**
 * HUD 渲染上下文；绘制能力经由 BaniraDrawContext 统一适配到当前版本。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraHudRenderContext {
    private final @Nonnull BaniraDrawContext draw;
    private final int screenWidth;
    private final int screenHeight;
    private final float partialTick;

    public BaniraHudRenderContext(@Nonnull BaniraDrawContext draw, int screenWidth, int screenHeight, float partialTick) {
        this.draw = draw;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.partialTick = partialTick;
    }
}
