package xin.vanilla.banira.common.config;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 将 {@link ConfigHolder} 的某一分类暴露为接口视图：path 规则为 {@code categoryPath + "." + 方法名}，
 * 与注解配置扫描 {@link xin.vanilla.banira.common.config.annotation.Config} 类时生成的路径一致。
 */
public final class ConfigCategoryViewProxy {

    /**
     * 读值归一化：处理 holder 为空、值为 null、以及与原手写 API 一致的空串回退等。
     */
    @FunctionalInterface
    public interface GetNormalizer {
        Object normalize(String leafName, @Nullable Object fromHolder, Object defaultsBean) throws Exception;
    }

    private ConfigCategoryViewProxy() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> sectionInterface, ConfigHolder holder, String categoryPath,
                               Object defaultsBean, GetNormalizer normalizer) {
        return (T) Proxy.newProxyInstance(sectionInterface.getClassLoader(), new Class<?>[]{sectionInterface},
                (proxy, method, args) -> handle(sectionInterface, categoryPath, holder, defaultsBean, normalizer,
                        proxy, method, args));
    }

    private static Object handle(Class<?> sectionInterface, String categoryPath, ConfigHolder holder,
                                 Object defaultsBean, GetNormalizer normalizer,
                                 Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() == Object.class) {
            return objectMethod(proxy, method, args, sectionInterface.getSimpleName());
        }
        String leaf = method.getName();
        int pc = method.getParameterCount();
        if (pc == 0) {
            Object raw = holder != null ? holder.get(categoryPath + "." + leaf) : null;
            try {
                return normalizer.normalize(leaf, raw, defaultsBean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (pc == 1) {
            if (holder != null) {
                holder.set(categoryPath + "." + leaf, args[0]);
            }
            return proxy;
        }
        throw new UnsupportedOperationException(method.toString());
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args, String tag) {
        String n = method.getName();
        switch (n) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return tag + "@" + System.identityHashCode(proxy);
        }
        throw new UnsupportedOperationException(method.toString());
    }
}
