package xin.vanilla.banira.internal.forge.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.config.BaniraConfigService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;

import java.nio.file.Path;

public final class ForgeBaniraPlatform implements BaniraPlatform {
    private final BaniraLifecycle lifecycle = new ForgeBaniraLifecycle();
    private final BaniraConfigService config = new ForgeBaniraConfigService();
    private final BaniraNetworkService network = new ForgeBaniraNetworkService();

    @Override
    public String loaderType() {
        return "forge";
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    @Override
    public boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public BaniraLifecycle lifecycle() {
        return lifecycle;
    }

    @Override
    public BaniraConfigService config() {
        return config;
    }

    @Override
    public BaniraNetworkService network() {
        return network;
    }
}
