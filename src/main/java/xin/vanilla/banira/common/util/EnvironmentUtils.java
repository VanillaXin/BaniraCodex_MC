package xin.vanilla.banira.common.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 运行环境判断
 * <p>
 * 注意：{@link EnvType} 表示当前加载的 JAR 侧（客户端或专用服务端），与游戏内逻辑侧
 * {@code World#isClientSide()} 不同；单机世界中服务端逻辑仍在客户端进程内运行。
 */
public final class EnvironmentUtils {
    private EnvironmentUtils() {
    }

    // region 物理分发（Dist）

    /**
     * 当前 JVM 对应的 Forge 物理分发（客户端或专用服务端）。
     */
    public static EnvType dist() {
        return FabricLoader.getInstance().getEnvironmentType();
    }

    /**
     * 是否为客户端分发（{@code runClient}、整合包客户端等）。
     */
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    /**
     * 是否为专用服务端分发（{@code runServer}、无头服务端等）。
     */
    public static boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    // endregion 物理分发（Dist）

    // region 发布 / 开发

    /**
     * 是否为发布环境（非开发环境；与 Gradle {@code runClient}/{@code runServer} 等开发运行相对）。
     */
    public static boolean isProduction() {
        return !FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /**
     * 是否为开发环境（{@link #isProduction()} 的否定）。
     */
    public static boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    // endregion 发布 / 开发
}
