package xin.vanilla.banira.common.network;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mod 安装状态声明的共享状态；公开注册入口位于 {@link xin.vanilla.banira.api.BaniraModPresence}。
 */
public final class ModLoadedPresenceStore {

    private static final ModLoadedPresenceRegistry REGISTRY = new ModLoadedPresenceRegistry();

    private ModLoadedPresenceStore() {
    }

    @Nonnull
    public static ModLoadedRegistration register(@Nonnull String modid, @Nonnull Consumer<Object> onServerSync) {
        return REGISTRY.register(modid, onServerSync);
    }

    @Nonnull
    public static ModLoadedRegistration register(@Nonnull String modid) {
        return register(modid, p -> {
        });
    }

    public static boolean unregister(@Nonnull String modid) {
        return REGISTRY.unregister(modid);
    }

    public static boolean hasRegistration(@Nonnull String modid) {
        return REGISTRY.hasRegistration(modid);
    }

    @Nonnull
    public static List<String> announcedModIds() {
        return REGISTRY.announcedModIds();
    }

    /**
     * 服务端：在已写入玩家 mod 安装状态后，执行该 mod 注册的同步回调（若存在）。
     */
    public static boolean dispatchServerSync(@Nonnull Object player, @Nonnull String modid) {
        return REGISTRY.dispatchServerSync(player, modid);
    }
}
