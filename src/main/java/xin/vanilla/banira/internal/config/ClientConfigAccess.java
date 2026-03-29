package xin.vanilla.banira.internal.config;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link ClientConfig} 运行时视图
 */
final class ClientConfigAccess {

    private static final EnumSeason DEFAULT_GUI_THEME_STYLE = EnumSeason.AUTO;
    private static final EnumGuiNightMode DEFAULT_GUI_NIGHT_MODE = EnumGuiNightMode.OFF;
    private static final int DEFAULT_NIGHT_START_MINUTE = 22 * 60;
    private static final int DEFAULT_NIGHT_END_MINUTE = 6 * 60;
    private static final int DEFAULT_NOTIFICATION_LOG_MAX_ENTRIES = 500;
    private static final boolean DEFAULT_USE_CUSTOM_CURSOR = true;

    private ClientConfigAccess() {
    }

    static ClientConfig.RootView root(ConfigHolder holder) {
        return (ClientConfig.RootView) Proxy.newProxyInstance(
                ClientConfig.class.getClassLoader(),
                new Class<?>[]{ClientConfig.RootView.class},
                (proxy, method, args) -> rootHandle(proxy, method, args, holder));
    }

    private static Object rootHandle(Object proxy, Method method, Object[] args, ConfigHolder holder) {
        if (method.getDeclaringClass() == Object.class) {
            String n = method.getName();
            switch (n) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "ClientConfig.RootView@" + System.identityHashCode(proxy);
                default:
                    throw new UnsupportedOperationException(method.toString());
            }
        }
        switch (method.getName()) {
            case "guiThemeStyle":
                if (args == null || args.length == 0) {
                    if (holder == null) {
                        return DEFAULT_GUI_THEME_STYLE;
                    }
                    return EnumSeason.valueOfDefault(holder.get("guiThemeStyle"));
                }
                if (holder != null) {
                    holder.set("guiThemeStyle", args[0]);
                }
                return proxy;
            case "guiNightMode":
                if (args == null || args.length == 0) {
                    if (holder == null) {
                        return DEFAULT_GUI_NIGHT_MODE;
                    }
                    return EnumGuiNightMode.valueOfDefault(holder.get("guiNightMode"));
                }
                if (holder != null) {
                    holder.set("guiNightMode", args[0]);
                }
                return proxy;
            case "guiNightModeStartMinute":
                if (args == null || args.length == 0) {
                    if (holder == null) {
                        return DEFAULT_NIGHT_START_MINUTE;
                    }
                    return intConfig(holder, "guiNightModeStartMinute", DEFAULT_NIGHT_START_MINUTE);
                }
                if (holder != null) {
                    holder.set("guiNightModeStartMinute", args[0]);
                }
                return proxy;
            case "guiNightModeEndMinute":
                if (args == null || args.length == 0) {
                    if (holder == null) {
                        return DEFAULT_NIGHT_END_MINUTE;
                    }
                    return intConfig(holder, "guiNightModeEndMinute", DEFAULT_NIGHT_END_MINUTE);
                }
                if (holder != null) {
                    holder.set("guiNightModeEndMinute", args[0]);
                }
                return proxy;
            case "notificationLogMaxEntries":
                if (args == null || args.length == 0) {
                    if (holder == null) {
                        return DEFAULT_NOTIFICATION_LOG_MAX_ENTRIES;
                    }
                    return intConfig(holder, "notificationLogMaxEntries", DEFAULT_NOTIFICATION_LOG_MAX_ENTRIES);
                }
                if (holder != null) {
                    holder.set("notificationLogMaxEntries", args[0]);
                }
                return proxy;
            case "useCustomCursor":
                if (args == null || args.length == 0) {
                    if (holder == null) {
                        return DEFAULT_USE_CUSTOM_CURSOR;
                    }
                    return boolConfig(holder, "useCustomCursor", DEFAULT_USE_CUSTOM_CURSOR);
                }
                if (holder != null) {
                    holder.set("useCustomCursor", args[0]);
                }
                return proxy;
            case "holder":
                return holder;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }

    private static int intConfig(ConfigHolder holder, String path, int def) {
        Object v = holder.get(path);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return def;
    }

    private static boolean boolConfig(ConfigHolder holder, String path, boolean def) {
        Object v = holder.get(path);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return def;
    }
}
