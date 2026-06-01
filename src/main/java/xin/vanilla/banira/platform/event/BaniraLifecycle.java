package xin.vanilla.banira.platform.event;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Minimal loader-neutral lifecycle callbacks.
 */
public interface BaniraLifecycle {
    void onCommonSetup(@Nonnull Runnable callback);

    void onServerStarting(@Nonnull Consumer<MinecraftServer> callback);

    void onServerTickEnd(@Nonnull Runnable callback);

    void onServerStopping(@Nonnull Consumer<MinecraftServer> callback);
}
