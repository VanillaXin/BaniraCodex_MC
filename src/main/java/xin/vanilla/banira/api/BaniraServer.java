package xin.vanilla.banira.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 子 mod 获取当前服务器的稳定入口；具体服务器类型由对应 MC 分支传入。
 */
public final class BaniraServer {
    private BaniraServer() {
    }

    public static boolean isRunning() {
        return Banira.platform().serverService().isRunning();
    }

    @Nullable
    public static Object current() {
        return Banira.platform().serverService().current();
    }

    @Nullable
    public static <T> T currentAs(@Nonnull Class<T> serverType) {
        Objects.requireNonNull(serverType, "serverType");
        Object server = current();
        return serverType.isInstance(server) ? serverType.cast(server) : null;
    }

    @Nonnull
    public static <T> T require(@Nonnull Class<T> serverType) {
        T server = currentAs(serverType);
        if (server == null) {
            throw new IllegalStateException("No running server for type: " + serverType.getName());
        }
        return server;
    }
}
