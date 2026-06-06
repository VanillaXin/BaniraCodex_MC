package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

/**
 * 客户端屏幕打开/切换事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraScreenOpenEvent {
    private final @Nullable Object screen;
    private final @Nullable Object nativeEvent;

    public BaniraScreenOpenEvent(@Nullable Object screen, @Nullable Object nativeEvent) {
        this.screen = screen;
        this.nativeEvent = nativeEvent;
    }
}
