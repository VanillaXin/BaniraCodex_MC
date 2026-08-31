package xin.vanilla.banira.internal.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * common 层访问客户端运行时的窄桥接点，避免专用服解析 client-only 类。
 */
public final class ClientRuntimeBridge {
    private static volatile Method localPlayerMethod;
    private static volatile Method playerMethod;
    private static volatile Method levelPlayerMethod;
    private static volatile Method onlinePlayerNameMethod;
    private static volatile Method onlinePlayerSkinMethod;
    private static volatile Method resourceManagerMethod;
    private static volatile Method selectedLanguageCodeMethod;

    private ClientRuntimeBridge() {
    }

    @Nullable
    public static Player localPlayer() {
        return playerValue(invoke("localPlayer"));
    }

    @Nullable
    public static Player player() {
        return playerValue(invoke("player"));
    }

    @Nullable
    public static Player levelPlayer(@Nullable UUID uuid) {
        return playerValue(invoke("levelPlayer", new Class<?>[]{UUID.class}, uuid));
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
    private static Player playerValue(Object value) {
        return value instanceof Player ? (Player) value : null;
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
        switch (methodName) {
            case "localPlayer": return localPlayerMethod;
            case "player": return playerMethod;
            case "levelPlayer": return levelPlayerMethod;
            case "onlinePlayerName": return onlinePlayerNameMethod;
            case "onlinePlayerSkin": return onlinePlayerSkinMethod;
            case "resourceManager": return resourceManagerMethod;
            case "selectedLanguageCode": return selectedLanguageCodeMethod;
            default: return null;
        }
    }

    private static void cacheMethod(String methodName, Method method) {
        switch (methodName) {
            case "localPlayer": localPlayerMethod = method; break;
            case "player": playerMethod = method; break;
            case "levelPlayer": levelPlayerMethod = method; break;
            case "onlinePlayerName": onlinePlayerNameMethod = method; break;
            case "onlinePlayerSkin": onlinePlayerSkinMethod = method; break;
            case "resourceManager": resourceManagerMethod = method; break;
            case "selectedLanguageCode": selectedLanguageCodeMethod = method; break;
            default: break;
        }
    }
}
