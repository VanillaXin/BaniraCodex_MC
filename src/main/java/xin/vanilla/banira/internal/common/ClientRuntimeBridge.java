package xin.vanilla.banira.internal.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * common 层访问客户端运行时的窄桥接点，避免服务端类加载阶段直接解析 client-only 类。
 */
public final class ClientRuntimeBridge {
    private static volatile Method localPlayerMethod;
    private static volatile Method levelMethod;
    private static volatile Method levelPlayerMethod;
    private static volatile Method onlinePlayerNameMethod;
    private static volatile Method onlinePlayerSkinMethod;
    private static volatile Method resourceManagerMethod;
    private static volatile Method selectedLanguageCodeMethod;

    private ClientRuntimeBridge() {
    }

    /**
     * 返回当前物理客户端玩家；专用服或客户端尚未初始化时返回 null。
     */
    @Nullable
    public static Player localPlayer() {
        Object value = invoke("localPlayer");
        return value instanceof Player ? (Player) value : null;
    }

    /** 返回当前客户端世界；服务端或世界尚未建立时返回 null。 */
    @Nullable
    public static Level level() {
        Object value = invoke("level");
        return value instanceof Level ? (Level) value : null;
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
    public static ResourceManager resourceManager() {
        Object value = invoke("resourceManager");
        return value instanceof ResourceManager ? (ResourceManager) value : null;
    }

    @Nullable
    public static String selectedLanguageCode() {
        Object value = invoke("selectedLanguageCode");
        return value instanceof String ? (String) value : null;
    }

    @Nullable
    private static Object invoke(String methodName) {
        return invoke(methodName, new Class<?>[0]);
    }

    @Nullable
    private static Object invoke(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!BaniraPlatforms.isInstalled() || !BaniraPlatforms.get().isClient()) {
            return null;
        }
        try {
            Method method = cachedMethod(methodName);
            if (method == null) {
                method = runtimeClass().getMethod(methodName, parameterTypes);
                cacheMethod(methodName, method);
            }
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Class<?> runtimeClass() throws ClassNotFoundException {
        return Class.forName("xin.vanilla.banira.internal.client.BaniraClientRuntime");
    }

    @Nullable
    private static Method cachedMethod(String methodName) {
        return switch (methodName) {
            case "localPlayer" -> localPlayerMethod;
            case "level" -> levelMethod;
            case "levelPlayer" -> levelPlayerMethod;
            case "onlinePlayerName" -> onlinePlayerNameMethod;
            case "onlinePlayerSkin" -> onlinePlayerSkinMethod;
            case "resourceManager" -> resourceManagerMethod;
            case "selectedLanguageCode" -> selectedLanguageCodeMethod;
            default -> null;
        };
    }

    private static void cacheMethod(String methodName, Method method) {
        switch (methodName) {
            case "localPlayer" -> localPlayerMethod = method;
            case "level" -> levelMethod = method;
            case "levelPlayer" -> levelPlayerMethod = method;
            case "onlinePlayerName" -> onlinePlayerNameMethod = method;
            case "onlinePlayerSkin" -> onlinePlayerSkinMethod = method;
            case "resourceManager" -> resourceManagerMethod = method;
            case "selectedLanguageCode" -> selectedLanguageCodeMethod = method;
            default -> {
            }
        }
    }
}
