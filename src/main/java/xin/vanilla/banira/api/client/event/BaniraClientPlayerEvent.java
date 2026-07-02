package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * 客户端玩家连接事件；只暴露稳定 Java 类型，避免公共 API 绑定不同版本的 Player 类。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraClientPlayerEvent {
    private final @Nonnull UUID uuid;
    private final @Nonnull String name;

    public BaniraClientPlayerEvent(@Nonnull UUID uuid, @Nonnull String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
