package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 客户端初始化事件；nativeEvent 仅用于当前分支内部迁移。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraClientSetupEvent {
    private final @Nonnull Object nativeEvent;

    public BaniraClientSetupEvent(@Nonnull Object nativeEvent) {
        this.nativeEvent = nativeEvent;
    }
}
