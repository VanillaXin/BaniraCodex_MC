package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 加载器无关的配置注册与访问服务。
 */
public interface BaniraConfigService {

    <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId);

    @Nonnull
    <T> T get(@Nonnull Class<T> configClass);

    @Nonnull
    default <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
        Object view = get(configClass);
        if (viewClass.isInstance(view)) {
            return viewClass.cast(view);
        }
        throw new IllegalStateException("Config view is not " + viewClass.getName() + ": " + view.getClass().getName());
    }

    @Nullable
    BaniraConfigHandle handle(@Nonnull Class<?> configClass);
}
