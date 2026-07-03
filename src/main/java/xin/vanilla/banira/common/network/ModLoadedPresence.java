package xin.vanilla.banira.common.network;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * 旧内部入口保留为薄 facade，实际状态统一由 ModLoadedPresenceStore 维护。
 */
public final class ModLoadedPresence {
    private ModLoadedPresence() {
    }

    public static void register(@Nonnull String modid, @Nonnull Consumer<ServerPlayer> onServerSync) {
        ModLoadedPresenceStore.register(modid, player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                onServerSync.accept(serverPlayer);
            }
        });
    }

    public static void register(@Nonnull String modid) {
        ModLoadedPresenceStore.register(modid);
    }

    @Nonnull
    public static List<String> announcedModIds() {
        return ModLoadedPresenceStore.announcedModIds();
    }

    public static void dispatchServerSync(@Nonnull ServerPlayer player, @Nonnull String modid) {
        ModLoadedPresenceStore.dispatchServerSync(player, modid);
    }
}
