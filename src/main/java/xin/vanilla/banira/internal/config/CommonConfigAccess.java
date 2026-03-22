package xin.vanilla.banira.internal.config;

import xin.vanilla.banira.common.config.ConfigCategoryViewProxy;
import xin.vanilla.banira.common.config.ConfigHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * {@link CommonConfig} 运行时视图
 */
final class CommonConfigAccess {

    private static final CommonConfig.HelpCategory DEFAULT_HELP = new CommonConfig.HelpCategory();
    private static final CommonConfig.LanguageCategory DEFAULT_LANGUAGE = new CommonConfig.LanguageCategory();
    private static final CommonConfig.CommandCategory DEFAULT_COMMAND = new CommonConfig.CommandCategory();
    private static final CommonConfig.PermissionCategory DEFAULT_PERMISSION = new CommonConfig.PermissionCategory();

    private CommonConfigAccess() {
    }

    static CommonConfig.RootView root(ConfigHolder holder) {
        return (CommonConfig.RootView) Proxy.newProxyInstance(
                CommonConfig.class.getClassLoader(),
                new Class<?>[]{CommonConfig.RootView.class},
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
                    return "CommonConfig.RootView@" + System.identityHashCode(proxy);
            }
            throw new UnsupportedOperationException(method.toString());
        }
        switch (method.getName()) {
            case "help":
                return ConfigCategoryViewProxy.create(CommonConfig.HelpView.class, holder, "help", DEFAULT_HELP,
                        CommonConfigAccess::readHelp);
            case "language":
                return ConfigCategoryViewProxy.create(CommonConfig.LanguageView.class, holder, "language", DEFAULT_LANGUAGE,
                        CommonConfigAccess::readLanguage);
            case "command":
                return ConfigCategoryViewProxy.create(CommonConfig.CommandView.class, holder, "command", DEFAULT_COMMAND,
                        CommonConfigAccess::readCommand);
            case "permission":
                return ConfigCategoryViewProxy.create(CommonConfig.PermissionView.class, holder, "permission",
                        DEFAULT_PERMISSION, CommonConfigAccess::readPermission);
            case "holder":
                return holder;
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }

    private static Object readHelp(String leaf, Object raw, Object bean) throws Exception {
        if ("helpHeader".equals(leaf)) {
            return raw;
        }
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readLanguage(String leaf, Object raw, Object bean) throws Exception {
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readCommand(String leaf, Object raw, Object bean) throws Exception {
        if ("commandLanguage".equals(leaf) || "commandVirtualOp".equals(leaf)) {
            return raw;
        }
        if ("commandPrefix".equals(leaf) || "commandHelp".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            String s = (String) raw;
            return s.isEmpty() ? field(bean, leaf) : s;
        }
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object readPermission(String leaf, Object raw, Object bean) throws Exception {
        if ("editServerConfigVirtualPermissionKey".equals(leaf)) {
            if (raw == null) {
                return field(bean, leaf);
            }
            String s = (String) raw;
            return s.isEmpty() ? field(bean, leaf) : s;
        }
        if (raw == null) {
            return field(bean, leaf);
        }
        return raw;
    }

    private static Object field(Object bean, String name) throws Exception {
        Field f = bean.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(bean);
    }
}
