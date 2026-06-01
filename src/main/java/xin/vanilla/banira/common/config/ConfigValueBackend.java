package xin.vanilla.banira.common.config;

import java.util.Set;

/**
 * Loader-specific storage backend used by {@link ConfigHolder}.
 */
public interface ConfigValueBackend {
    Set<String> getValuePaths();

    Object get(String path);

    void set(String path, Object value);

    void save();
}
