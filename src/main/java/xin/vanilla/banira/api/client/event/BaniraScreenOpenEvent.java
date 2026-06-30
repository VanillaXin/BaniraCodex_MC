package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 客户端屏幕打开/切换事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraScreenOpenEvent {
    private final @Nonnull BaniraScreenInfo screen;

    public BaniraScreenOpenEvent(@Nonnull BaniraScreenInfo screen) {
        this.screen = screen;
    }
}
