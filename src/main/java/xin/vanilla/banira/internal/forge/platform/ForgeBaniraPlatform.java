package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.internal.common.BaniraNotificationServices;
import xin.vanilla.banira.internal.common.BaniraPaths;
import xin.vanilla.banira.internal.forge.config.ForgeBaniraConfigService;
import xin.vanilla.banira.internal.forge.network.ForgeBaniraNetworkService;
import xin.vanilla.banira.platform.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge 1.18.2 的基础 platform 实现。
 */
public final class ForgeBaniraPlatform implements BaniraPlatform {
    private final BaniraInputService input = createInputService();
    private final BaniraLogoService logo = createLogoService();

    @Nonnull
    @Override
    public String loaderType() {
        return "forge";
    }

    @Nonnull
    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
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
    public boolean isModLoaded(@Nonnull String modId) {
        return !modId.isEmpty() && ModList.get().isLoaded(modId);
    }

    @Nonnull
    @Override
    public String modDisplayName(@Nonnull String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Nullable
    @Override
    public String lastKnownUsername(@Nonnull UUID uuid) {
        return UsernameCache.getLastKnownUsername(uuid);
    }

    @Nonnull
    @Override
    public String modIdFromMainClass(@Nonnull Class<?> modMainClass) {
        Mod mod = modMainClass.getAnnotation(Mod.class);
        if (mod == null || mod.value() == null || mod.value().trim().isEmpty()) {
            throw new IllegalArgumentException("Class must be annotated with a loader mod entry annotation: " + modMainClass.getName());
        }
        return mod.value();
    }

    @Nonnull
    @Override
    public Class<?> modMainClass(@Nonnull String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getMod())
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .orElseThrow(() -> new IllegalStateException("No loaded mod main class for mod id: " + modId));
    }

    @Nonnull
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Nonnull
    @Override
    public BaniraPathService pathService() {
        return BaniraPaths.SERVICE;
    }

    @Nonnull
    @Override
    public BaniraConfigService configService() {
        return ForgeBaniraConfigService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraServerService serverService() {
        return ForgeBaniraServerService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraPlayerDataService playerDataService() {
        return ForgeBaniraPlayerDataService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraPermissionService permissionService() {
        return ForgeBaniraPermissionService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraNetworkService networkService() {
        return ForgeBaniraNetworkService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraRegistryService registryService() {
        return ForgeBaniraRegistryService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraInputService inputService() {
        return input;
    }

    @Nonnull
    @Override
    public BaniraNotificationService notificationService() {
        return BaniraNotificationServices.INSTANCE;
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

    /** 客户端实现只在确认运行侧后解析，专用服务器不会链接这些类。 */
    private static final class ClientServices {
        private static BaniraInputService input() {
            return xin.vanilla.banira.internal.forge.client.ForgeKeyBindingService.INSTANCE;
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
