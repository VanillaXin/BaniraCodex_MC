package xin.vanilla.banira.platform;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.client.BaniraApiInputBridge;
import xin.vanilla.banira.internal.config.BaniraConfigHandleAdapter;
import xin.vanilla.banira.platform.client.BaniraClientService;
import xin.vanilla.banira.platform.command.BaniraCommandService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;
import xin.vanilla.banira.platform.registry.BaniraRegistryService;
import xin.vanilla.banira.platform.resource.BaniraResourceService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Loader-neutral platform surface exposed to dependent mods.
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
                return self.server().worldDataPath(rootDirectoryName());
            }

            @Override
            public Path playerDataPath() {
                Path worldDataPath = worldDataPath();
                return worldDataPath != null ? worldDataPath.resolve("playerdata") : null;
            }

            @Override
            public Path vanillaPlayerDataPath() {
                return self.server().worldPlayerDataPath();
            }
        };
    }

    /**
     * 当前加载器的客户端输入服务。
     */
    default BaniraInputService inputService() {
        return BaniraApiInputBridge.service();
    }

    BaniraLifecycle lifecycle();

    xin.vanilla.banira.platform.config.BaniraConfigService config();

    /**
     * 根级配置服务是新版公共入口，旧的 platform.config 服务仅作为当前分支内部实现保留。
     */
    default BaniraConfigService configService() {
        BaniraPlatform self = this;
        return new BaniraConfigService() {
            @Override
            public <T> void register(Class<T> configClass, String modId) {
                self.config().register(configClass, modId);
            }

            @Override
            public <T> T get(Class<T> configClass) {
                return self.config().get(configClass);
            }

            @Override
            public BaniraConfigHandle handle(Class<?> configClass) {
                ConfigHolder holder = self.config().getHolder(configClass);
                return holder != null ? new BaniraConfigHandleAdapter(holder) : null;
            }
        };
    }

    BaniraCommandService command();

    BaniraClientService client();

    BaniraNetworkService network();

    BaniraRegistryService registry();

    BaniraWorldService world();

    BaniraServerService server();

    BaniraResourceService resources();
}
