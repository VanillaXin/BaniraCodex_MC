package xin.vanilla.banira.internal.forge.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Forge 内部字段访问工具，用于 Mixin 难以覆盖的加载器元数据对象。
 */
public final class ForgeInternalFieldAccess {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Object UNSAFE = resolveUnsafe();
    private static final Method OBJECT_FIELD_OFFSET = resolveUnsafeMethod("objectFieldOffset", Field.class);
    private static final Method STATIC_FIELD_BASE = resolveUnsafeMethod("staticFieldBase", Field.class);
    private static final Method STATIC_FIELD_OFFSET = resolveUnsafeMethod("staticFieldOffset", Field.class);
    private static final Method PUT_OBJECT = resolveUnsafeMethod("putObject", Object.class, long.class, Object.class);

    private ForgeInternalFieldAccess() {
    }

    /**
     * 读取指定字段；只在 Forge internal adapter 中使用。
     */
    public static Object readField(Class<?> owner, Object instance, String fieldName) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    /**
     * 写入普通或 final 对象字段；final fallback 仅用于 loader 元数据等无法 Mixin 的场景。
     */
    public static boolean writeObjectField(Class<?> owner, Object instance, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (!Modifier.isFinal(field.getModifiers())) {
                field.set(instance, value);
                return true;
            }
            if (clearFinalModifier(field)) {
                try {
                    field.set(instance, value);
                    return true;
                } catch (IllegalAccessException ignored) {
                }
            }
            return putObjectWithUnsafe(instance, field, value);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("Failed to write internal Forge field {}.{}", owner.getName(), fieldName, e);
            return false;
        }
    }

    private static boolean clearFinalModifier(Field field) {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean putObjectWithUnsafe(Object instance, Field field, Object value) {
        if (UNSAFE == null || PUT_OBJECT == null || field.getType().isPrimitive()) {
            return false;
        }
        try {
            Object base;
            long offset;
            if (Modifier.isStatic(field.getModifiers())) {
                if (STATIC_FIELD_BASE == null || STATIC_FIELD_OFFSET == null) {
                    return false;
                }
                base = STATIC_FIELD_BASE.invoke(UNSAFE, field);
                offset = (Long) STATIC_FIELD_OFFSET.invoke(UNSAFE, field);
            } else {
                if (OBJECT_FIELD_OFFSET == null || instance == null) {
                    return false;
                }
                base = instance;
                offset = (Long) OBJECT_FIELD_OFFSET.invoke(UNSAFE, field);
            }
            PUT_OBJECT.invoke(UNSAFE, base, offset, value);
            return true;
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("Unsafe fallback failed for internal Forge field {}", field.getName(), e);
            return false;
        }
    }

    private static Object resolveUnsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method resolveUnsafeMethod(String name, Class<?>... parameterTypes) {
        if (UNSAFE == null) {
            return null;
        }
        try {
            Method method = UNSAFE.getClass().getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
