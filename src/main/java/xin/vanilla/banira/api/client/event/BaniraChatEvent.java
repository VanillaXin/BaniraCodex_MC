package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 客户端聊天发送事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraChatEvent {
    private final @Nonnull String message;

    public BaniraChatEvent(@Nonnull String message) {
        this.message = message;
    }
}
