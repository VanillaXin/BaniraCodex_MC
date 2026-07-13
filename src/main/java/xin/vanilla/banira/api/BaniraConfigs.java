package xin.vanilla.banira.api;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 子 mod 推荐使用的配置注册与访问入口。
 */
public final class BaniraConfigs {

    private BaniraConfigs() {
    }

    public static <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        Banira.platform().configService().register(configClass, modId);
    }

    @Nonnull
    public static <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
        return Banira.platform().configService().view(configClass, viewClass);
    }

    /**
     * 返回加载器无关句柄；GUI 或内部编辑器需要的详细元数据仍由 common.config.ConfigHolder 承载。
     */
    @Nullable
    public static BaniraConfigHandle handle(@Nonnull Class<?> configClass) {
        return Banira.platform().configService().handle(configClass);
    }

    /**
     * 返回配置编辑器需要的完整公共模型；普通读写优先使用 {@link #handle(Class)}。
     */
    @Nullable
    public static ConfigHolder holder(@Nonnull Class<?> configClass) {
        BaniraConfigHandle handle = handle(configClass);
        if (handle == null) {
            return null;
        }
        if (handle instanceof ConfigHolder) {
            return (ConfigHolder) handle;
        }
        throw new IllegalStateException("Config handle is not a ConfigHolder: " + handle.getClass().getName());
    }

    /**
     * 获取已注册配置的句柄；未注册时直接抛出异常，便于在 mod 初始化阶段暴露错误。
     */
    @Nonnull
    public static BaniraConfigHandle requireHandle(@Nonnull Class<?> configClass) {
        BaniraConfigHandle handle = handle(configClass);
        if (handle == null) {
            throw new IllegalStateException("Config not registered: " + configClass.getName());
        }
        return handle;
    }

    @Nullable
    public static String findValuePath(@Nonnull Class<?> configClass, @Nonnull String key) {
        return requireHandle(configClass).findValuePath(key);
    }

    public static boolean hasValue(@Nonnull Class<?> configClass, @Nonnull String path) {
        return requireHandle(configClass).hasValue(path);
    }

    @Nullable
    public static Object defaultValue(@Nonnull Class<?> configClass, @Nonnull String path) {
        return requireHandle(configClass).defaultValue(path);
    }

    @Nullable
    public static <T> T get(@Nonnull Class<?> configClass, @Nonnull String path) {
        return requireHandle(configClass).get(path);
    }

    public static void set(@Nonnull Class<?> configClass, @Nonnull String path, @Nullable Object value) {
        requireHandle(configClass).set(path, value);
    }

    public static boolean setIfValid(@Nonnull Class<?> configClass, @Nonnull String path, @Nullable Object value) {
        return requireHandle(configClass).setIfValid(path, value);
    }

    public static void save(@Nonnull Class<?> configClass) {
        requireHandle(configClass).save();
    }

    public static void setAndSave(@Nonnull Class<?> configClass, @Nonnull String path, @Nullable Object value) {
        BaniraConfigHandle handle = requireHandle(configClass);
        handle.set(Objects.requireNonNull(path, "path"), value);
        handle.save();
    }
}
