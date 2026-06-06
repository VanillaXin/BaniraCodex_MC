package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 客户端屏幕事件；screen 为当前分支原生 Screen 对象。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraScreenEvent {
    private final @Nonnull Object screen;
    private final @Nonnull Object nativeEvent;

    public BaniraScreenEvent(@Nonnull Object screen, @Nonnull Object nativeEvent) {
        this.screen = screen;
        this.nativeEvent = nativeEvent;
    }
}
