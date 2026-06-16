package xin.vanilla.banira.internal.fabric.platform;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.SharedConstants;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.internal.common.BaniraPaths;
import xin.vanilla.banira.internal.fabric.client.FabricKeyBindingService;
import xin.vanilla.banira.internal.fabric.config.FabricBaniraConfigService;
import xin.vanilla.banira.internal.fabric.network.FabricBaniraNetworkService;
import xin.vanilla.banira.platform.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Fabric 1.19.2 的 platform 实现。
 */
public final class FabricBaniraPlatform implements BaniraPlatform {

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
        if (modMainClass == BaniraCodex.class) {
            return Banira.MOD_ID;
        }
        throw new UnsupportedOperationException("Fabric does not expose a stable mod id lookup from class: " + modMainClass.getName());
    }

    @Nonnull
    @Override
    public Class<?> modMainClass(@Nonnull String modId) {
        ModContainer container = FabricLoader.getInstance().getModContainer(modId)
                .orElseThrow(() -> new IllegalStateException("No loaded mod for id: " + modId));
        throw new UnsupportedOperationException("Fabric does not expose a stable loaded main class for " + container.getMetadata().getId());
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
    public BaniraNetworkService networkService() {
        return FabricBaniraNetworkService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraRegistryService registryService() {
        return NoopRegistryService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraInputService inputService() {
        return FabricKeyBindingService.INSTANCE;
    }
}
