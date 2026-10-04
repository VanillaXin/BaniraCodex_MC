package xin.vanilla.banira.common.config;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * 配置值的加载器无关存取接口。
 */
public interface ConfigValueStore {

    Set<String> paths();

    @Nullable
    Object get(String path);

    /**
     * Compare stored values without exposing them; no display or runtime type conversion.
     */
    default boolean matchesStoredValue(String path, Object expected) {
        return paths().contains(path) && java.util.Objects.deepEquals(get(path), expected);
    }

    /**
     * Freeze expectations once, but read current values on every call; enum names are opt-in.
     */
    default BooleanSupplier prepareStoredMatch(Map<String, Object> expected, boolean allowEnumNames) {
        ConfigValueExpectation expectation = new ConfigValueExpectation(expected, allowEnumNames);
        return () -> {
            for (int i = 0; i < expectation.size(); i++) {
                String path = expectation.path(i);
                if (!paths().contains(path) || !expectation.matches(i, get(path))) return false;
            }
            return true;
        };
    }

    void set(String path, Object value);

    /**
     * Apply a validated batch without partial writes; unsupported backends must opt in.
     */
    default void setAll(Map<String, Object> changes) {
        throw new UnsupportedOperationException("Batch config edits are not supported");
    }

    default ConfigEditSnapshot snapshotForEdit(Set<String> paths) {
        throw new UnsupportedOperationException("Local config snapshots are not supported");
    }

    default ConfigReadSnapshot snapshotForRead(Set<String> paths) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (String path : paths) {
            if (!paths().contains(path)) throw new IllegalArgumentException("Unknown config path: " + path);
            values.put(path, get(path));
        }
        return ConfigReadSnapshot.of(values);
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
