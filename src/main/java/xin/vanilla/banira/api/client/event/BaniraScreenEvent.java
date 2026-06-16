package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 客户端屏幕事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraScreenEvent {
    private final @Nonnull BaniraScreenInfo screen;

    public BaniraScreenEvent(@Nonnull BaniraScreenInfo screen) {
        this.screen = screen;
    }
}
