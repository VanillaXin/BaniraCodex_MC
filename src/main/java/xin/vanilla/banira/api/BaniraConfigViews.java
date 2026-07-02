package xin.vanilla.banira.api;

import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * 基于 {@link BaniraConfigHandle} 创建配置视图代理，供子 mod 避免直接依赖 ConfigHolder。
 */
public final class BaniraConfigViews {

    /**
     * 读取值归一化钩子，用于补默认值、空字符串兼容或枚举转换。
     */
    @FunctionalInterface
    public interface GetNormalizer {
        @Nullable
        Object normalize(@Nonnull String leafName, @Nullable Object value, @Nullable Object defaultsBean) throws Exception;
    }

    private BaniraConfigViews() {
    }

    @Nonnull
    public static <T> T category(@Nonnull Class<T> sectionInterface, @Nonnull Class<?> configClass,
                                 @Nonnull String categoryPath, @Nullable Object defaultsBean,
                                 @Nonnull GetNormalizer normalizer) {
        return category(sectionInterface, BaniraConfigs.requireHandle(configClass), categoryPath, defaultsBean, normalizer);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> T category(@Nonnull Class<T> sectionInterface, @Nonnull BaniraConfigHandle handle,
                                 @Nonnull String categoryPath, @Nullable Object defaultsBean,
                                 @Nonnull GetNormalizer normalizer) {
        Objects.requireNonNull(sectionInterface, "sectionInterface");
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(categoryPath, "categoryPath");
        Objects.requireNonNull(normalizer, "normalizer");
        if (!sectionInterface.isInterface()) {
            throw new IllegalArgumentException("Config view must be an interface: " + sectionInterface.getName());
        }
        return (T) Proxy.newProxyInstance(sectionInterface.getClassLoader(), new Class<?>[]{sectionInterface},
                (proxy, method, args) -> handleMethod(sectionInterface, categoryPath, handle, defaultsBean, normalizer,
                        proxy, method, args));
    }

    private static Object handleMethod(Class<?> sectionInterface, String categoryPath, BaniraConfigHandle handle,
                                       @Nullable Object defaultsBean, GetNormalizer normalizer,
                                       Object proxy, Method method, @Nullable Object[] args) throws Exception {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args, sectionInterface.getSimpleName());
        }
        int parameterCount = method.getParameterCount();
        String path = path(categoryPath, method.getName());
        if (parameterCount == 0) {
            return normalizer.normalize(method.getName(), handle.get(path), defaultsBean);
        }
        if (parameterCount == 1) {
            handle.set(path, args == null ? null : args[0]);
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static String path(String categoryPath, String leafName) {
        return categoryPath.isEmpty() ? leafName : categoryPath + "." + leafName;
    }

    private static Object objectMethod(Object proxy, Method method, @Nullable Object[] args, String tag) {
        switch (method.getName()) {
            case "equals":
                return args != null && args.length == 1 && proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return tag + "@" + System.identityHashCode(proxy);
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }
}
