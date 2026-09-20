package xin.vanilla.banira.common.config.view;

import xin.vanilla.banira.common.config.annotation.ConfigEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Immutable metadata shared by a generated root and its category views. */
public final class ConfigViewField<T> {
    final String path;
    final Field declaration;
    final Class<?> valueType;
    final Class<?> elementType;
    final Function<Object, T> parser;
    final boolean keepNull;
    final boolean defaultEmpty;

    private ConfigViewField(String path, Class<?> owner, String fieldName, Class<?> valueType,
                            Class<?> elementType, Function<Object, T> parser) {
        this.path = Objects.requireNonNull(path, "path");
        if (path.isEmpty() || path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
            throw new IllegalArgumentException("Invalid config path: " + path);
        }
        try {
            declaration = Objects.requireNonNull(owner, "owner").getDeclaredField(fieldName);
            declaration.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Missing config declaration: " + path, e);
        }
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.elementType = elementType;
        this.parser = parser;
        if (Modifier.isStatic(declaration.getModifiers())
                || ConfigViewValues.box(declaration.getType()) != valueType) {
            throw new IllegalArgumentException("Config declaration type mismatch: " + path);
        }
        if (elementType != null) {
            Type generic = declaration.getGenericType();
            if (!(generic instanceof ParameterizedType)
                    || ((ParameterizedType) generic).getActualTypeArguments()[0] != elementType) {
                throw new IllegalArgumentException("Config list element type mismatch: " + path);
            }
        }
        Class<?> scalarType = elementType == null ? valueType : elementType;
        if (!ConfigViewValues.supports(scalarType) || parser != null && !scalarType.isEnum()) {
            throw new IllegalArgumentException("Unsupported config field type: " + path);
        }
        ConfigEntry.Access access = declaration.getAnnotation(ConfigEntry.Access.class);
        keepNull = access != null && access.nulls() == ConfigEntry.Access.NullPolicy.KEEP;
        defaultEmpty = access != null && access.emptyString() == ConfigEntry.Access.EmptyPolicy.DEFAULT;
        if (keepNull && declaration.getType().isPrimitive() || defaultEmpty && valueType != String.class) {
            throw new IllegalArgumentException("Invalid config access policy: " + path);
        }
        if (access != null && !access.enumParser().isEmpty() && parser == null) {
            throw new IllegalArgumentException("Missing generated enum parser: " + path);
        }
    }

    public static <T> ConfigViewField<T> scalar(String path, Class<?> owner, String fieldName, Class<T> valueType) {
        return scalar(path, owner, fieldName, valueType, null);
    }

    public static <T> ConfigViewField<T> scalar(String path, Class<?> owner, String fieldName,
                                               Class<T> valueType, Function<Object, T> parser) {
        return new ConfigViewField<>(path, owner, fieldName, valueType, null, parser);
    }

    public static <E> ConfigViewField<List<E>> list(String path, Class<?> owner, String fieldName, Class<E> elementType) {
        return new ConfigViewField<>(path, owner, fieldName, List.class,
                Objects.requireNonNull(elementType, "elementType"), null);
    }
}
