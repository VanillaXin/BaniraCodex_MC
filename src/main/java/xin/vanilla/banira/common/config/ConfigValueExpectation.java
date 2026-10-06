package xin.vanilla.banira.common.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Frozen stored values shared by native and generic prepared comparisons.
 */
public final class ConfigValueExpectation {
    private final String[] paths;
    private final Object[] values;
    private final boolean allowEnumNames;

    public ConfigValueExpectation(Map<String, Object> expected, boolean allowEnumNames) {
        Map<String, Object> frozen = ConfigEditSnapshot.immutableValues(expected);
        paths = frozen.keySet().toArray(new String[0]);
        values = frozen.values().toArray();
        this.allowEnumNames = allowEnumNames;
    }

    public int size() {
        return paths.length;
    }

    public String path(int index) {
        return paths[index];
    }

    public boolean matches(int index, Object current) {
        Object expected = values[index];
        return storedEquals(current, expected)
                || allowEnumNames && expected instanceof Enum && ((Enum<?>) expected).name().equals(current);
    }

    public static boolean storedEquals(Object current, Object expected) {
        if (current == expected) return true;
        // Java 8 array-backed List.equals creates iterators on every check.
        if (current instanceof List && current instanceof RandomAccess
                && expected instanceof List && expected instanceof RandomAccess) {
            List<?> left = (List<?>) current, right = (List<?>) expected;
            if (left.size() != right.size()) return false;
            for (int i = 0; i < left.size(); i++) {
                if (!storedEquals(left.get(i), right.get(i))) return false;
            }
            return true;
        }
        return Objects.deepEquals(current, expected);
    }
}
