package xin.vanilla.banira.api.client.hud;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * HUD 渲染上下文；nativeGraphics 只给当前分支内部或临时迁移代码使用。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraHudRenderContext {
    private final @Nonnull Object nativeGraphics;
    private final int screenWidth;
    private final int screenHeight;
    private final float partialTick;

    public BaniraHudRenderContext(@Nonnull Object nativeGraphics, int screenWidth, int screenHeight, float partialTick) {
        this.nativeGraphics = nativeGraphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.partialTick = partialTick;
    }
}
