package xin.vanilla.banira.api;

import xin.vanilla.banira.common.network.ModLoadedPresenceStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * 子 mod 用于声明“客户端也安装了本 mod”的稳定入口。
 */
public final class BaniraModPresence {

    private BaniraModPresence() {
    }

    @Nonnull
    public static BaniraModPresenceRegistration register(@Nonnull String modId, @Nonnull Consumer<Object> onServerSync) {
        return ModLoadedPresenceStore.register(modId, onServerSync)::close;
    }

    @Nonnull
    public static BaniraModPresenceRegistration register(@Nonnull String modId) {
        return register(modId, player -> {
        });
    }

    public static boolean unregister(@Nonnull String modId) {
        return ModLoadedPresenceStore.unregister(modId);
    }

    public static boolean hasRegistration(@Nonnull String modId) {
        return ModLoadedPresenceStore.hasRegistration(modId);
    }

    @Nonnull
    public static List<String> announcedModIds() {
        return ModLoadedPresenceStore.announcedModIds();
    }
}
