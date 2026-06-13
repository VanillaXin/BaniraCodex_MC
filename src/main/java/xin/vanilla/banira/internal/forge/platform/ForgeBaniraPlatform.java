package xin.vanilla.banira.internal.forge.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.client.BaniraClientService;
import xin.vanilla.banira.platform.command.BaniraCommandService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;
import xin.vanilla.banira.platform.registry.BaniraRegistryService;
import xin.vanilla.banira.platform.resource.BaniraResourceService;
import xin.vanilla.banira.platform.server.BaniraServerService;
import xin.vanilla.banira.platform.world.BaniraWorldService;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public final class ForgeBaniraPlatform implements BaniraPlatform {
    private final BaniraLifecycle lifecycle = new ForgeBaniraLifecycle();
    private final BaniraConfigService config = new ForgeBaniraConfigService();
    private final BaniraCommandService command = new ForgeBaniraCommandService();
    private final BaniraClientService client = DistExecutor.safeRunForDist(
            () -> xin.vanilla.banira.internal.forge.client.ForgeBaniraClientService::new,
            () -> BaniraClientService::noop);
    private final BaniraNetworkService network = new ForgeBaniraNetworkService();
    private final BaniraRegistryService registry = new ForgeBaniraRegistryService();
    private final BaniraWorldService world = new ForgeBaniraWorldService();
    private final BaniraServerService server = new ForgeBaniraServerService();
    private final BaniraResourceService resources = new ForgeBaniraResourceService();

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
    public boolean isModLoaded(String modId) {
        return modId != null && ModList.get().isLoaded(modId);
    }

    @Override
    public String modDisplayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Override
    public String modIdFromMainClass(Class<?> modMainClass) {
        Mod mod = modMainClass != null ? modMainClass.getAnnotation(Mod.class) : null;
        if (mod == null || mod.value() == null || mod.value().trim().isEmpty()) {
            throw new IllegalArgumentException("Class must be annotated with a loader mod entry annotation: " + modMainClass);
        }
        return mod.value();
    }

    @Override
    public Class<?> modMainClass(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getMod())
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .orElseThrow(() -> new IllegalStateException("No loaded mod main class for mod id: " + modId));
    }

    @Override
    public String lastKnownUsername(UUID uuid) {
        return uuid != null ? UsernameCache.getLastKnownUsername(uuid) : null;
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
    public BaniraConfigService configService() {
        return config;
    }

    @Override
    public BaniraCommandService command() {
        return command;
    }

    @Override
    public BaniraClientService client() {
        return client;
    }

    @Override
    public BaniraNetworkService network() {
        return network;
    }

    @Override
    public BaniraRegistryService registry() {
        return registry;
    }

    @Override
    public BaniraWorldService world() {
        return world;
    }

    @Override
    public BaniraServerService server() {
        return server;
    }

    @Override
    public BaniraResourceService resources() {
        return resources;
    }
}
