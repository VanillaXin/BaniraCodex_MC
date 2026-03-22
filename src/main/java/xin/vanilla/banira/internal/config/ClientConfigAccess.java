package xin.vanilla.banira.internal.config;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link ClientConfig} 运行时视图
 */
final class ClientConfigAccess {

    private static final EnumSeason DEFAULT_GUI_THEME_STYLE = EnumSeason.AUTO;

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
            case "holder":
                return holder;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }
}
