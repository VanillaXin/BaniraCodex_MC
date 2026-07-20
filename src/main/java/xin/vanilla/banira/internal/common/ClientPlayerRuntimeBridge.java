package xin.vanilla.banira.internal.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享玩家工具访问客户端状态的窄桥接，专用服不会解析任何 client-only 类型。
 */
public final class ClientPlayerRuntimeBridge {
    private static final String RUNTIME_CLASS = "xin.vanilla.banira.internal.client.BaniraClientPlayerRuntime";
    private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();

    private ClientPlayerRuntimeBridge() {
    }

    @Nullable
    public static Player localPlayer() {
        Object value = invoke("localPlayer", new Class<?>[0]);
        return value instanceof Player ? (Player) value : null;
    }

    @Nullable
    public static Player levelPlayer(@Nullable UUID uuid) {
        Object value = invoke("levelPlayer", new Class<?>[]{UUID.class}, uuid);
        return value instanceof Player ? (Player) value : null;
    }

    @Nullable
    public static String onlinePlayerName(@Nullable UUID uuid) {
        Object value = invoke("onlinePlayerName", new Class<?>[]{UUID.class}, uuid);
        return value instanceof String ? (String) value : null;
    }

    @Nullable
    public static ResourceLocation onlinePlayerSkin(@Nullable UUID uuid) {
        Object value = invoke("onlinePlayerSkin", new Class<?>[]{UUID.class}, uuid);
        return value instanceof ResourceLocation ? (ResourceLocation) value : null;
    }

    @Nullable
    private static Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!BaniraPlatforms.isInstalled() || !BaniraPlatforms.get().isClient()) return null;
        try {
            String key = methodName + ':' + parameterTypes.length;
            Method method = METHODS.get(key);
            if (method == null) {
                method = Class.forName(RUNTIME_CLASS).getMethod(methodName, parameterTypes);
                METHODS.put(key, method);
            }
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
