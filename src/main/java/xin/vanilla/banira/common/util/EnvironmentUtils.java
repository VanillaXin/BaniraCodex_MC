package xin.vanilla.banira.common.util;

import xin.vanilla.banira.api.Banira;

/**
 * 运行环境判断
 */
public final class EnvironmentUtils {
    private EnvironmentUtils() {
    }

    // region 物理分发（Dist）

    /**
     * 是否为客户端分发（{@code runClient}、整合包客户端等）。
     */
    public static boolean isClient() {
        return Banira.platform().isClient();
    }

    /**
     * 是否为专用服务端分发（{@code runServer}、无头服务端等）。
     */
    public static boolean isDedicatedServer() {
        return Banira.platform().isDedicatedServer();
    }

    // endregion 物理分发（Dist）

    // region 发布 / 开发

    /**
     * 是否为发布环境（非开发环境；与 Gradle {@code runClient}/{@code runServer} 等开发运行相对）。
     */
    public static boolean isProduction() {
        return !isDevelopment();
    }

    /**
     * 是否为开发环境（{@link #isProduction()} 的否定）。
     */
    public static boolean isDevelopment() {
        return Banira.platform().isDevelopment();
    }

    // endregion 发布 / 开发
}
