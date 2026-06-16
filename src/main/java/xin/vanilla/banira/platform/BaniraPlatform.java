package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 子 mod 面向的稳定 platform 入口；加载器和 MC 版本差异留在实现层。
 */
public interface BaniraPlatform {
    @Nonnull
    String loaderType();

    @Nonnull
    String minecraftVersion();

    boolean isClient();

    boolean isDedicatedServer();

    boolean isDevelopment();

    boolean isModLoaded(@Nonnull String modId);

    @Nonnull
    String modDisplayName(@Nonnull String modId);

    @Nonnull
    String modIdFromMainClass(@Nonnull Class<?> modMainClass);

    @Nonnull
    Class<?> modMainClass(@Nonnull String modId);

    @Nullable
    String lastKnownUsername(@Nonnull UUID uuid);

    @Nonnull
    Path configDir();

    /**
     * 当前加载器和 MC 版本的数据路径服务。
     */
    @Nonnull
    BaniraPathService pathService();

    /**
     * 当前加载器的客户端输入服务。
     */
    @Nonnull
    BaniraInputService inputService();

    /**
     * 根级配置服务；子 mod 只通过该入口注册、读取配置。
     */
    @Nonnull
    BaniraConfigService configService();

    /**
     * 根级网络服务；具体加载器和 MC 版本差异留在实现层。
     */
    @Nonnull
    BaniraNetworkService networkService();

    /**
     * 根级注册表服务；具体加载器和 MC 版本差异留在实现层。
     */
    @Nonnull
    BaniraRegistryService registryService();
}
