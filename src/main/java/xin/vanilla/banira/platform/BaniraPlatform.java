package xin.vanilla.banira.platform;

import xin.vanilla.banira.platform.config.BaniraConfigService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;
import xin.vanilla.banira.platform.registry.BaniraRegistryService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Loader-neutral platform surface exposed to dependent mods.
 */
public interface BaniraPlatform {
    String loaderType();

    boolean isClient();

    boolean isDedicatedServer();

    boolean isDevelopment();

    boolean isModLoaded(String modId);

    String modDisplayName(String modId);

    String modIdFromMainClass(Class<?> modMainClass);

    Class<?> modMainClass(String modId);

    String lastKnownUsername(UUID uuid);

    Path configDir();

    BaniraLifecycle lifecycle();

    BaniraConfigService config();

    BaniraNetworkService network();

    BaniraRegistryService registry();

    BaniraWorldService world();

    BaniraServerService server();
}
