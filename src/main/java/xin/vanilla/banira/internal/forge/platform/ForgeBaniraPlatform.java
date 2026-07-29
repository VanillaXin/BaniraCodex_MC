package xin.vanilla.banira.internal.forge.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.internal.common.BaniraNotificationServices;
import xin.vanilla.banira.platform.*;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ForgeBaniraPlatform implements BaniraPlatform {
    private final BaniraPathService path = new ForgeBaniraPathService(this::configDir);
    private final BaniraInputService input = createInputService();
    private final BaniraConfigService config = new ForgeBaniraConfigService();
    private final ForgeBaniraServerService server = new ForgeBaniraServerService();
    private final BaniraPlayerDataService playerData = ForgeBaniraPlayerDataService.INSTANCE;
    private final BaniraNetworkService network = new ForgeBaniraNetworkService();
    private final BaniraRegistryService registry = new ForgeBaniraRegistryService();
    private final BaniraNotificationService notification = BaniraNotificationServices.INSTANCE;
    private final BaniraLogoService logo = createLogoService();

    @Override
    public String loaderType() {
        return "forge";
    }

    @Override
    public String minecraftVersion() {
        return "1.16.5";
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
    public BaniraPathService pathService() {
        return path;
    }

    @Override
    public BaniraInputService inputService() {
        return input;
    }

    @Override
    public BaniraConfigService configService() {
        return config;
    }

    @Override
    public BaniraServerService serverService() {
        return server;
    }

    @Override
    public BaniraPlayerDataService playerDataService() {
        return playerData;
    }

    @Override
    public BaniraNetworkService networkService() {
        return network;
    }

    @Override
    public BaniraRegistryService registryService() {
        return registry;
    }

    @Nonnull
    @Override
    public BaniraNotificationService notificationService() {
        return notification;
    }

    @Nonnull
    @Override
    public BaniraLogoService logoService() {
        return logo;
    }

    private static BaniraInputService createInputService() {
        return FMLEnvironment.dist == Dist.CLIENT
                ? ClientServices.input()
                : ServerInputService.INSTANCE;
    }

    private static BaniraLogoService createLogoService() {
        return FMLEnvironment.dist == Dist.CLIENT
                ? ClientServices.logo()
                : ServerLogoService.INSTANCE;
    }

    /**
     * 客户端类只在确认运行侧后由这个嵌套桥接加载。
     */
    private static final class ClientServices {
        private static BaniraInputService input() {
            return xin.vanilla.banira.internal.client.BaniraApiInputBridge.service();
        }

        private static BaniraLogoService logo() {
            return xin.vanilla.banira.internal.forge.client.ForgeLogoService.INSTANCE;
        }
    }

    private enum ServerInputService implements BaniraInputService {
        INSTANCE;

        @Nonnull
        @Override
        public BaniraKeyHandle register(@Nonnull BaniraKeySpec spec) {
            throw new IllegalStateException("Client input is unavailable on a dedicated server");
        }

        @Override
        public boolean isKeyDown(int keyCode) {
            return false;
        }

        @Override
        public boolean isMouseDown(int button) {
            return false;
        }

        @Override
        public void flushPendingRegistrations() {
        }
    }

    private enum ServerLogoService implements BaniraLogoService {
        INSTANCE;

        @Override
        public void register(@Nonnull String modId, @Nonnull Supplier<String> logoFileSupplier) {
        }

        @Override
        public void register(@Nonnull Function<String, String> logoFileFunction) {
        }
    }
}
