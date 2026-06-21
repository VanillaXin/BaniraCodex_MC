package xin.vanilla.banira.common.network;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端向服务端声明「已安装某 mod」时的注册表。
 * <p>
 * 在 {@link xin.vanilla.banira.api.event.BaniraLifecycle#onCommonSetup(java.util.function.Consumer)} 中调用 {@link #register}，
 * 客户端进入世界时会自动将已注册的 modid 打包上报；
 * 服务端收到后为每个 modid 更新 {@link xin.vanilla.banira.common.util.PlayerUtils} 中「远程客户端」安装状态，并执行对应的数据同步回调；
 * 随后服务端会向客户端回传本端 mod 列表，由客户端写入「远程服务端」状态。
 */
public final class ModLoadedPresence {

    private static final ModLoadedPresenceRegistry REGISTRY = new ModLoadedPresenceRegistry();

    private ModLoadedPresence() {
    }

    /**
     * 注册需要在客户端登录时向服务端声明的 mod，并在服务端收到声明后执行同步逻辑。
     *
     * @param modid        mod 标识
     * @param onServerSync 服务端收到该玩家已安装此 mod 后的回调（例如在此时下发自定义同步数据）
     */
    @Nonnull
    public static ModLoadedRegistration register(@Nonnull String modid, @Nonnull Consumer<Object> onServerSync) {
        return REGISTRY.register(modid, onServerSync);
    }

    /**
     * 仅声明客户端安装了该 mod（服务端仍会更新 {@link xin.vanilla.banira.common.util.PlayerUtils} 中远程客户端状态），无额外同步回调。
     */
    @Nonnull
    public static ModLoadedRegistration register(@Nonnull String modid) {
        return register(modid, p -> {
        });
    }

    /**
     * 注销指定 modid 的声明，主要用于开发环境热重载或临时声明。
     */
    public static boolean unregister(@Nonnull String modid) {
        return REGISTRY.unregister(modid);
    }

    public static boolean hasRegistration(@Nonnull String modid) {
        return REGISTRY.hasRegistration(modid);
    }

    /**
     * 按注册顺序返回需要向服务端上报的 modid 列表（供客户端发包使用）。
     */
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
