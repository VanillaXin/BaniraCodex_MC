package xin.vanilla.banira.platform;

import xin.vanilla.banira.internal.client.BaniraApiInputBridge;
import xin.vanilla.banira.internal.server.BaniraServerAccess;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 子 mod 面向的稳定 platform 入口；加载器和 MC 版本差异留在实现层。
 */
public interface BaniraPlatform {
    String loaderType();

    default String minecraftVersion() {
        return "1.16.5";
    }

    boolean isClient();

    boolean isDedicatedServer();

    boolean isDevelopment();

    boolean isModLoaded(String modId);

    String modDisplayName(String modId);

    String modIdFromMainClass(Class<?> modMainClass);

    Class<?> modMainClass(String modId);

    String lastKnownUsername(UUID uuid);

    Path configDir();

    /**
     * 当前加载器和 MC 版本的数据路径服务。
     */
    default BaniraPathService pathService() {
        BaniraPlatform self = this;
        return new BaniraPathService() {
            @Override
            public String rootDirectoryName() {
                return "vanilla.xin";
            }

            @Override
            public Path configPath() {
                return self.configDir().resolve(rootDirectoryName());
            }

            @Override
            public Path worldDataPath() {
                Path path = BaniraServerAccess.worldDataPath(rootDirectoryName());
                return path != null ? path : Paths.get("world", rootDirectoryName());
            }

            @Override
            public Path playerDataPath() {
                Path worldDataPath = worldDataPath();
                return worldDataPath != null ? worldDataPath.resolve("playerdata") : null;
            }

            @Override
            public Path vanillaPlayerDataPath() {
                Path path = BaniraServerAccess.worldPlayerDataPath();
                return path != null ? path : Paths.get("world", "playerdata");
            }
        };
    }

    /**
     * 当前加载器的客户端输入服务。
     */
    default BaniraInputService inputService() {
        return BaniraApiInputBridge.service();
    }

    /**
     * 根级配置服务；子 mod 只通过该入口注册、读取配置。
     */
    BaniraConfigService configService();

    /**
     * 根级网络服务；具体加载器和 MC 版本差异留在实现层。
     */
    BaniraNetworkService networkService();

    /**
     * 根级注册表服务；具体加载器和 MC 版本差异留在实现层。
     */
    BaniraRegistryService registryService();
}
