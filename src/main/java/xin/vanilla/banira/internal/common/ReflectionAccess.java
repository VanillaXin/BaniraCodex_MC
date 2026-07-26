package xin.vanilla.banira.internal.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Banira 内部普通反射工具；不负责绕过 final/record 等加载器私有实现限制。
 */
public final class ReflectionAccess {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private ReflectionAccess() {
    }

    @Nullable
    public static Class<?> classOf(@Nullable Object value) {
        return value == null ? null : value.getClass();
    }

    @Nullable
    public static Class<?> classByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Failed to get class {}", className, e);
            return null;
        }
    }

    public static List<String> privateFieldNames(Class<?> clazz, Class<?> target) {
        return privateFieldNames(clazz, target, false, false, false);
    }

    public static List<String> privateFieldNames(Class<?> clazz, Class<?> target, boolean parent,
                                                 boolean targetFrom, boolean targetInstance) {
        List<String> fieldNames = new ArrayList<>();
        if (clazz == null || target == null) {
            return fieldNames;
        }
        Class<?> cur = clazz;
        try {
            do {
                for (Field field : cur.getDeclaredFields()) {
                    if ((Modifier.isPrivate(field.getModifiers()) || Modifier.isProtected(field.getModifiers()))
                            && (field.getType() == target
                            || (targetFrom && target.isAssignableFrom(field.getType()))
                            || (targetInstance && target.isAssignableFrom(field.getType())))) {
                        fieldNames.add(field.getName());
                    }
                }
                cur = cur.getSuperclass();
            } while (parent && cur != null && cur != Object.class);
        } catch (Exception e) {
            LOGGER.error("Failed to get private field names from {}", cur == null ? clazz.getName() : cur.getName(), e);
        }
        return fieldNames;
    }

    public static List<String> fieldNames(Class<?> clazz, boolean includeParent, Predicate<Field> fieldPredicate) {
        List<String> fieldNames = new ArrayList<>();
        if (clazz == null || fieldPredicate == null) {
            return fieldNames;
        }
        Class<?> cur = clazz;
        try {
            do {
                for (Field field : cur.getDeclaredFields()) {
                    if (fieldPredicate.test(field)) {
                        fieldNames.add(field.getName());
                    }
                }
                cur = cur.getSuperclass();
            } while (includeParent && cur != null && cur != Object.class);
        } catch (Exception e) {
            LOGGER.error("Failed to get field names from {}", cur == null ? clazz.getName() : cur.getName(), e);
        }
        return fieldNames;
    }

    @Nullable
    public static Object fieldValue(Class<?> clazz, Object instance, String fieldName) {
        return fieldValue(clazz, instance, fieldName, false);
    }

    @Nullable
    public static Object fieldValue(Class<?> clazz, Object instance, String fieldName, boolean parent) {
        Field field = findField(clazz, fieldName, parent);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to get private field {} from {}", fieldName, clazz.getName(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T fieldValue(Class<?> clazz, Object instance, String fieldName, boolean parent, Class<T> type) {
        Object value = fieldValue(clazz, instance, fieldName, parent);
        if (value == null) {
            return null;
        }
        if (type != null && !type.isInstance(value)) {
            LOGGER.error("Field {} in {} is not of expected type {}", fieldName, clazz.getName(), type.getName());
            return null;
        }
        return (T) value;
    }

    @Nullable
    public static <T> T fieldValue(Object instance, String fieldName, Class<T> type) {
        Objects.requireNonNull(instance, "instance");
        return fieldValue(instance.getClass(), instance, fieldName, true, type);
    }

    public static boolean setFieldValue(Class<?> clazz, Object instance, String fieldName, Object value) {
        return setFieldValue(clazz, instance, fieldName, value, false);
    }

    public static boolean setFieldValue(Class<?> clazz, Object instance, String fieldName, Object value, boolean parent) {
        Field field = findField(clazz, fieldName, parent);
        if (field == null) {
            LOGGER.error("Failed to locate private field {} on {}", fieldName, clazz == null ? "(null)" : clazz.getName());
            return false;
        }
        return setFieldValue(field, instance, value);
    }

    public static boolean setFieldValue(Field field, Object instance, Object value) {
        try {
            int modifiers = field.getModifiers();
            if (Modifier.isFinal(modifiers) && (Modifier.isStatic(modifiers) || field.getDeclaringClass().isRecord())) {
                LOGGER.warn("Refusing to mutate immutable field {}.{}", field.getDeclaringClass().getName(), field.getName());
                return false;
            }
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to set private field {} from {}", field.getName(), field.getDeclaringClass().getName(), e);
            return false;
        }
    }

    @Nullable
    public static Field findField(Class<?> clazz, String fieldName) {
        return findField(clazz, fieldName, true);
    }

    @Nullable
    public static Field findField(Class<?> clazz, String fieldName, boolean parent) {
        if (clazz == null || fieldName == null) {
            return null;
        }
        Map<String, Field> byName = FIELD_CACHE.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        Field cached = byName.get(fieldName);
        if (cached != null) {
            return cached;
        }
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            try {
                Field field = cur.getDeclaredField(fieldName);
                field.setAccessible(true);
                byName.put(fieldName, field);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
            if (!parent) {
                break;
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    @Nullable
    public static Object newInstanceFromClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            LOGGER.error("Failed to create instance of class {}", className, e);
            return null;
        }
    }

    @Nullable
    public static Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        if (clazz == null || methodName == null || args == null) {
            return null;
        }
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            for (Method method : cur.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length
                        && directParametersMatch(method.getParameterTypes(), args)) {
                    method.setAccessible(true);
                    return method;
                }
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    public static MethodMatchResult findMethodWithTypeConversion(Class<?> clazz, String methodName, Object[] args) {
        if (clazz == null || methodName == null || args == null) {
            return new MethodMatchResult(null, null);
        }
        Method directMatch = findMethod(clazz, methodName, args);
        if (directMatch != null) {
            return new MethodMatchResult(directMatch, args);
        }
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            for (Method method : cur.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                    Object[] convertedArgs = convertArgsToMatchTypes(args, method.getParameterTypes());
                    if (convertedArgs != null) {
                        method.setAccessible(true);
                        return new MethodMatchResult(method, convertedArgs);
                    }
                }
            }
            cur = cur.getSuperclass();
        }
        return new MethodMatchResult(null, null);
    }

    public static final class MethodMatchResult {
        public final Method method;
        public final Object[] args;

        public MethodMatchResult(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    private static boolean directParametersMatch(Class<?>[] paramTypes, Object[] args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] != null && !isDirectInvocationCompatible(args[i].getClass(), paramTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDirectInvocationCompatible(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) {
            return true;
        }
        if (!to.isPrimitive()) {
            return false;
        }
        if (to == boolean.class) return from == Boolean.class;
        if (to == byte.class) return from == Byte.class;
        if (to == short.class) return from == Short.class || from == Byte.class;
        if (to == int.class)
            return from == Integer.class || from == Short.class || from == Byte.class || from == Character.class;
        if (to == long.class)
            return from == Long.class || from == Integer.class || from == Short.class || from == Byte.class || from == Character.class;
        if (to == float.class)
            return from == Float.class || from == Long.class || from == Integer.class || from == Short.class || from == Byte.class || from == Character.class;
        if (to == double.class)
            return from == Double.class || from == Float.class || from == Long.class || from == Integer.class || from == Short.class || from == Byte.class || from == Character.class;
        return to == char.class && from == Character.class;
    }

    @Nullable
    private static Object[] convertArgsToMatchTypes(Object[] args, Class<?>[] targetTypes) {
        if (args.length != targetTypes.length) {
            return null;
        }
        Object[] converted = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object convertedArg = convertToType(args[i], targetTypes[i]);
            if (convertedArg == null && args[i] != null) {
                return null;
            }
            converted[i] = convertedArg;
        }
        return converted;
    }

    @Nullable
    private static Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            return defaultValue(targetType);
        }
        Class<?> valueClass = value.getClass();
        if (targetType.isAssignableFrom(valueClass)) {
            return value;
        }
        if (value instanceof Number) {
            return convertNumber((Number) value, targetType);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }
        if (targetType == char.class || targetType == Character.class) {
            if (value instanceof Character) {
                return value;
            }
            if (value instanceof String) {
                String text = (String) value;
                return text.isEmpty() ? '\u0000' : text.charAt(0);
            }
        }
        if (targetType == String.class) {
            return value.toString();
        }
        return null;
    }

    @Nullable
    private static Object defaultValue(Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return null;
        }
        if (targetType == boolean.class) return false;
        if (targetType == byte.class) return (byte) 0;
        if (targetType == short.class) return (short) 0;
        if (targetType == int.class) return 0;
        if (targetType == long.class) return 0L;
        if (targetType == float.class) return 0.0f;
        if (targetType == double.class) return 0.0D;
        if (targetType == char.class) return '\u0000';
        return null;
    }

    @Nullable
    private static Object convertNumber(Number number, Class<?> targetType) {
        if (targetType == Byte.class || targetType == byte.class) return number.byteValue();
        if (targetType == Short.class || targetType == short.class) return number.shortValue();
        if (targetType == Integer.class || targetType == int.class) return number.intValue();
        if (targetType == Long.class || targetType == long.class) return number.longValue();
        if (targetType == Float.class || targetType == float.class) return number.floatValue();
        if (targetType == Double.class || targetType == double.class) return number.doubleValue();
        return null;
    }

    private static boolean isCompatibleType(Class<?> from, Class<?> to) {
        if (to.isAssignableFrom(from)) return true;
        if (to == int.class) return from == Integer.class || from == Double.class || from == Float.class;
        if (to == double.class) return from == Double.class || from == Integer.class || from == Float.class;
        if (to == float.class) return from == Float.class || from == Double.class || from == Integer.class;
        if (to == boolean.class) return from == Boolean.class;
        if (to == long.class)
            return from == Long.class || from == Double.class || from == Float.class || from == Integer.class;
        if (to == char.class) return from == Character.class || from == Integer.class;
        if (to == byte.class) return from == Byte.class || from == Integer.class;
        if (to == short.class) return from == Short.class || from == Integer.class;
        return to == String.class && from == String.class;
    }
}
