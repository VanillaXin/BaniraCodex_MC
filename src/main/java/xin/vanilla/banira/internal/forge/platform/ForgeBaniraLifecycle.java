package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.platform.event.BaniraLifecycle;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

final class ForgeBaniraLifecycle implements BaniraLifecycle {
    @Override
    public void onCommonSetup(@Nonnull Runnable callback) {
        BaniraEventBus.ModLifecycle.onCommonSetup(callback);
    }

    @Override
    public void onServerStarting(@Nonnull Consumer<MinecraftServer> callback) {
        BaniraEventBus.Server.onStarting(callback);
    }

    @Override
    public void onServerTickEnd(@Nonnull Runnable callback) {
        BaniraEventBus.Server.onTickEnd(callback);
    }

    @Override
    public void onServerStopping(@Nonnull Consumer<MinecraftServer> callback) {
        BaniraEventBus.Server.onStopping(callback);
    }
}
