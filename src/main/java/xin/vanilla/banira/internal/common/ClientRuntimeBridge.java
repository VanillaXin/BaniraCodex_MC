package xin.vanilla.banira.internal.common;

import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * common 层访问客户端运行时的窄桥接点，避免服务端类加载阶段直接解析 client-only 类。
 */
public final class ClientRuntimeBridge {
    private static volatile Method localPlayerMethod;

    private ClientRuntimeBridge() {
    }

    /**
     * 返回当前物理客户端玩家；专用服或客户端尚未初始化时返回 null。
     */
    @Nullable
    public static Player localPlayer() {
        if (!BaniraPlatforms.isInstalled() || !BaniraPlatforms.get().isClient()) {
            return null;
        }
        try {
            Method method = localPlayerMethod;
            if (method == null) {
                Class<?> runtime = Class.forName("xin.vanilla.banira.internal.client.BaniraClientRuntime");
                method = runtime.getMethod("localPlayer");
                localPlayerMethod = method;
            }
            Object value = method.invoke(null);
            return value instanceof Player ? (Player) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
