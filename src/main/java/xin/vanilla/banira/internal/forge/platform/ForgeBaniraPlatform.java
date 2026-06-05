package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import xin.vanilla.banira.platform.BaniraPlatform;

import javax.annotation.Nonnull;
import java.nio.file.Path;

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

    @Nonnull
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
