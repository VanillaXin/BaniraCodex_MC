package xin.vanilla.banira.platform;

import xin.vanilla.banira.platform.config.BaniraConfigService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;

import java.nio.file.Path;

/**
 * Loader-neutral platform surface exposed to dependent mods.
 */
public interface BaniraPlatform {
    String loaderType();

    boolean isClient();

    boolean isDedicatedServer();

    boolean isDevelopment();

    Path configDir();

    BaniraLifecycle lifecycle();

    BaniraConfigService config();

    BaniraNetworkService network();
}
