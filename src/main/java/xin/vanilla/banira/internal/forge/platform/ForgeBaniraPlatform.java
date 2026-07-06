package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import xin.vanilla.banira.internal.common.BaniraNotificationServices;
import xin.vanilla.banira.internal.common.BaniraPaths;
import xin.vanilla.banira.internal.forge.client.ForgeKeyBindingService;
import xin.vanilla.banira.internal.forge.config.ForgeBaniraConfigService;
import xin.vanilla.banira.internal.forge.network.ForgeBaniraNetworkService;
import xin.vanilla.banira.platform.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Forge 1.18.2 的基础 platform 实现。
 */
public final class ForgeBaniraPlatform implements BaniraPlatform {

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
        return ForgeKeyBindingService.INSTANCE;
    }

    @Nonnull
    @Override
    public BaniraNotificationService notificationService() {
        return BaniraNotificationServices.INSTANCE;
    }
}
