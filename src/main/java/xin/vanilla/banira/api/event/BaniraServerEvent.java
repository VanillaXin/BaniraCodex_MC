package xin.vanilla.banira.api.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 服务端生命周期事件；server 是版本内部对象，公共 API 只承诺稳定事件语义。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraServerEvent {
    @Nullable
    private final Object server;

    public BaniraServerEvent(@Nullable Object server) {
        this.server = server;
    }

    public boolean hasServer() {
        return server != null;
    }

    @Nullable
    public <T> T serverAs(@Nonnull Class<T> type) {
        return type.isInstance(server) ? type.cast(server) : null;
    }
}
