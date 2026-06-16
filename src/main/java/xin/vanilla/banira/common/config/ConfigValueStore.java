package xin.vanilla.banira.common.config;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * 配置值的加载器无关存取接口。
 */
public interface ConfigValueStore {

    Set<String> paths();

    @Nullable
    Object get(String path);

    void set(String path, Object value);

    Class<?> valueClass(String path);

    @Nullable
    Object defaultValue(String path);

    boolean validate(String path, Object value);

    void save();
}
