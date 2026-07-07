package xin.vanilla.banira.common.util;

import xin.vanilla.banira.internal.common.ReflectionAccess;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

/**
 * 子 mod 可用的普通反射工具；不绕过 final/record 等 JVM 限制。
 */
public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    @Nullable
    public static Class<?> getClass(Object value) {
        return ReflectionAccess.classOf(value);
    }

    @Nullable
    public static Class<?> getClass(String className) {
        return ReflectionAccess.classByName(className);
    }

    public static List<String> getPrivateFieldNames(Class<?> clazz, Class<?> target) {
        return ReflectionAccess.privateFieldNames(clazz, target);
    }

    public static List<String> getPrivateFieldNames(Class<?> clazz, Class<?> target, boolean parent, boolean targetFrom, boolean targetInstance) {
        return ReflectionAccess.privateFieldNames(clazz, target, parent, targetFrom, targetInstance);
    }

    public static List<String> getFieldNames(Class<?> clazz, boolean includeParent, Predicate<Field> fieldPredicate) {
        return ReflectionAccess.fieldNames(clazz, includeParent, fieldPredicate);
    }

    @Nullable
    public static Object getPrivateFieldValue(Class<?> clazz, Object instance, String fieldName) {
        return ReflectionAccess.fieldValue(clazz, instance, fieldName);
    }

    @Nullable
    public static Object getPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, boolean parent) {
        return ReflectionAccess.fieldValue(clazz, instance, fieldName, parent);
    }

    @Nullable
    public static <T> T getPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, boolean parent, Class<T> type) {
        return ReflectionAccess.fieldValue(clazz, instance, fieldName, parent, type);
    }

    @Nullable
    public static <T> T getPrivateFieldValue(Object instance, String fieldName, Class<T> type) {
        return ReflectionAccess.fieldValue(instance, fieldName, type);
    }

    public static void setPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, Object value) {
        ReflectionAccess.setFieldValue(clazz, instance, fieldName, value);
    }

    public static void setPrivateFieldValue(Class<?> clazz, Object instance, String fieldName, Object value, boolean parent) {
        ReflectionAccess.setFieldValue(clazz, instance, fieldName, value, parent);
    }

    @Nullable
    public static Field findField(Class<?> clazz, String fieldName) {
        return ReflectionAccess.findField(clazz, fieldName);
    }

    @Nullable
    public static Object newInstanceFromClassName(String className) {
        return ReflectionAccess.newInstanceFromClassName(className);
    }

    @Nullable
    public static Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        return ReflectionAccess.findMethod(clazz, methodName, args);
    }

    public static MethodMatchResult findMethodWithTypeConversion(Class<?> clazz, String methodName, Object[] args) {
        ReflectionAccess.MethodMatchResult result = ReflectionAccess.findMethodWithTypeConversion(clazz, methodName, args);
        return new MethodMatchResult(result.method, result.args);
    }

    /**
     * 智能方法匹配结果，包含匹配到的方法和可能经过类型转换的参数。
     */
    public static final class MethodMatchResult {
        public final Method method;
        public final Object[] args;

        public MethodMatchResult(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }
}
