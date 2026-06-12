package xin.vanilla.banira.platform;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 加载器无关的配置运行时句柄；GUI 专用元数据仍由内部配置模型承载。
 */
public interface BaniraConfigHandle {
    String getModId();

    String getConfigName();

    void save();

    <T> T get(String path);

    void set(String path, Object value);

    Set<String> valuePaths();

    boolean hasValue(String path);

    @Nullable
    String findValuePath(String key);

    Class<?> valueClass(String path);

    @Nullable
    Object defaultValue(String path);

    boolean validate(String path, Object value);

    boolean setIfValid(String path, Object value);
}
