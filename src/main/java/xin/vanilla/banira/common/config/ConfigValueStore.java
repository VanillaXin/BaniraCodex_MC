package xin.vanilla.banira.common.config;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.Map;

/**
 * 配置值的加载器无关存取接口。
 */
public interface ConfigValueStore {

    Set<String> paths();

    @Nullable
    Object get(String path);

    /** Compare stored values without exposing them; no display or runtime type conversion. */
    default boolean matchesStoredValue(String path, Object expected) {
        return paths().contains(path) && java.util.Objects.deepEquals(get(path), expected);
    }

    void set(String path, Object value);

    /** Apply a validated batch without partial writes; unsupported backends must opt in. */
    default void setAll(Map<String, Object> changes) {
        throw new UnsupportedOperationException("Batch config edits are not supported");
    }

    default ConfigEditSnapshot snapshotForEdit(Set<String> paths) {
        throw new UnsupportedOperationException("Local config snapshots are not supported");
    }

    default ConfigCommitResult compareAndSetAll(ConfigEditSnapshot expected, Map<String, Object> changes) {
        throw new UnsupportedOperationException("Compare-and-commit config edits are not supported");
    }

    Class<?> valueClass(String path);

    @Nullable
    Object defaultValue(String path);

    boolean validate(String path, Object value);

    void save();
}
