package xin.vanilla.banira.internal.fabric.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.SharedConstants;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.internal.common.BaniraNotificationServices;
import xin.vanilla.banira.internal.common.BaniraPaths;
import xin.vanilla.banira.internal.fabric.client.FabricKeyBindingService;
import xin.vanilla.banira.internal.fabric.config.FabricBaniraConfigService;
import xin.vanilla.banira.internal.fabric.network.FabricBaniraNetworkService;
import xin.vanilla.banira.platform.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric 1.21.1 的 platform 实现。
 */
public final class FabricBaniraPlatform implements BaniraPlatform {
    private static final List<String> COMMON_ENTRYPOINT_KEYS = List.of("main");
    private static final List<String> CLIENT_ENTRYPOINT_KEYS = List.of("main", "client");
    private static final List<String> SERVER_ENTRYPOINT_KEYS = List.of("main", "server");

    private final Map<String, Class<?>> mainClassesByModId = new ConcurrentHashMap<>();
    private final Map<Class<?>, String> modIdsByMainClass = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public String loaderType() {
        return "fabric";
    }

    @Nonnull
    @Override
    public String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER;
    }

    @Override
    public boolean isDevelopment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(@Nonnull String modId) {
        return !modId.isEmpty() && FabricLoader.getInstance().isModLoaded(modId);
    }

    @Nonnull
    @Override
    public String modDisplayName(@Nonnull String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    @Nullable
    @Override
    public String lastKnownUsername(@Nonnull UUID uuid) {
        return null;
    }

    @Nonnull
    @Override
    public String modIdFromMainClass(@Nonnull Class<?> modMainClass) {
        Objects.requireNonNull(modMainClass, "modMainClass");
        if (modMainClass == BaniraCodex.class) {
            cacheEntrypoint(Banira.MOD_ID, BaniraCodex.class);
            return Banira.MOD_ID;
        }
        String cached = modIdsByMainClass.get(modMainClass);
        if (cached != null) {
            return cached;
        }
        refreshEntrypointClassIndex();
        cached = modIdsByMainClass.get(modMainClass);
        if (cached != null) {
            return cached;
        }
        throw new IllegalArgumentException("No Fabric entrypoint class is registered for: " + modMainClass.getName());
    }

    @Nonnull
    @Override
    public Class<?> modMainClass(@Nonnull String modId) {
        Objects.requireNonNull(modId, "modId");
        if (Banira.MOD_ID.equals(modId)) {
            cacheEntrypoint(Banira.MOD_ID, BaniraCodex.class);
            return BaniraCodex.class;
        }
        FabricLoader.getInstance().getModContainer(modId)
                .orElseThrow(() -> new IllegalStateException("No loaded mod for id: " + modId));
        Class<?> cached = mainClassesByModId.get(modId);
        if (cached != null) {
            return cached;
        }
        refreshEntrypointClassIndex();
        cached = mainClassesByModId.get(modId);
        if (cached != null) {
            return cached;
        }
        throw new IllegalStateException("No Fabric main/client/server entrypoint class for mod id: " + modId);
    }

    @Nonnull
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Nonnull
    @Override
    public BaniraPathService pathService() {
        return BaniraPaths.SERVICE;
    }

    @Nonnull
    @Override
    public BaniraConfigService configService() {
        return FabricBaniraConfigService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraServerService serverService() {
        return FabricBaniraServerService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraPlayerDataService playerDataService() {
        return FabricBaniraPlayerDataService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraNetworkService networkService() {
        return FabricBaniraNetworkService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraRegistryService registryService() {
        return FabricBaniraRegistryService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraInputService inputService() {
        return FabricKeyBindingService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraNotificationService notificationService() {
        return BaniraNotificationServices.INSTANCE;
    }

    private void refreshEntrypointClassIndex() {
        FabricLoader loader = FabricLoader.getInstance();
        for (String key : entrypointKeys()) {
            for (EntrypointContainer<Object> container : loader.getEntrypointContainers(key, Object.class)) {
                ModContainer provider = container.getProvider();
                Object entrypoint = container.getEntrypoint();
                if (provider != null && entrypoint != null) {
                    cacheEntrypoint(provider.getMetadata().getId(), entrypoint.getClass());
                }
            }
        }
    }

    private List<String> entrypointKeys() {
        if (isClient()) {
            return CLIENT_ENTRYPOINT_KEYS;
        }
        if (isDedicatedServer()) {
            return SERVER_ENTRYPOINT_KEYS;
        }
        return COMMON_ENTRYPOINT_KEYS;
    }

    private void cacheEntrypoint(String modId, Class<?> entrypointClass) {
        // Fabric 没有 Forge @Mod 那样的主类注解，只能用当前环境已注册的 entrypoint 做映射。
        mainClassesByModId.putIfAbsent(modId, entrypointClass);
        modIdsByMainClass.putIfAbsent(entrypointClass, modId);
    }
}
