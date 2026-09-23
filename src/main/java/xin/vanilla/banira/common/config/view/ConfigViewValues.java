package xin.vanilla.banira.common.config.view;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class ConfigViewValues {
    private ConfigViewValues() { }

    static Class<?> box(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        return type;
    }

    static boolean supports(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Double.class
                || type == Boolean.class || type == String.class || type.isEnum();
    }

    @SuppressWarnings("unchecked")
    static <T> T read(ConfigViewField<T> field, Object value, Supplier<Object> defaults) {
        if (value == null && field.keepNull) return null;
        if (value == null || field.defaultEmpty && "".equals(value)) {
            return fallback(field, defaults);
        }
        if (field.parser != null) {
            T parsed = field.parser.apply(value);
            return parsed == null ? fallback(field, defaults) : parsed;
        }
        if (field.elementType != null) {
            return value instanceof List ? (T) copyList(field, (List<?>) value) : fallback(field, defaults);
        }
        Object converted = scalar(field.valueType, value);
        return converted == null ? fallback(field, defaults) : (T) converted;
    }

    @SuppressWarnings("unchecked")
    private static <T> T fallback(ConfigViewField<T> field, Supplier<Object> defaults) {
        Object value = defaults.get();
        if (value == null) {
            if (field.declaration.getType().isPrimitive()) {
                throw new IllegalStateException("Missing primitive config default: " + field.path);
            }
            return null;
        }
        if (field.elementType != null && value instanceof List) return (T) copyList(field, (List<?>) value);
        Object converted = scalar(field.valueType, value);
        if (converted == null) throw new IllegalStateException("Invalid config default: " + field.path);
        return (T) converted;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object scalar(Class<?> type, Object value) {
        if (type.isInstance(value)) return value;
        if (value instanceof Number) {
            if (type == Integer.class) return ((Number) value).intValue();
            if (type == Long.class) return ((Number) value).longValue();
            if (type == Double.class) return ((Number) value).doubleValue();
        }
        if (type.isEnum() && value instanceof String) {
            try {
                return Enum.valueOf((Class) type, (String) value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static List<?> copyList(ConfigViewField<?> field, List<?> source) {
        List<Object> copy = new ArrayList<>(source.size());
        for (Object item : source) {
            Object converted = scalar(field.elementType, item);
            if (converted == null) {
                throw new IllegalStateException("Invalid config list element: " + field.path);
            }
            copy.add(converted);
        }
        return copy;
    }

    static Object write(ConfigViewField<?> field, Object value) {
        if (value == null) return null;
        if (!field.valueType.isInstance(value)) {
            throw new IllegalArgumentException("Invalid config value type: " + field.path);
        }
        if (field.elementType == null) return value;
        List<?> source = (List<?>) value;
        for (Object item : source) {
            if (!field.elementType.isInstance(item)) {
                throw new IllegalArgumentException("Invalid config list element: " + field.path);
            }
        }
        return new ArrayList<>(source);
    }
}
