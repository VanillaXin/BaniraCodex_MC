package xin.vanilla.banira.internal.neoforge.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.internal.common.ReflectionAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * NeoForge 内部对象字段写入点；用于 ModInfo 等 Mixin 够不到或不适合改的加载器元数据。
 */
public final class NeoForgeInternalFieldAccess {
    private static final Logger LOGGER = LogManager.getLogger();

    private NeoForgeInternalFieldAccess() {
    }

    public static boolean setObjectField(Class<?> ownerClass, Object instance, String fieldName, Object value) {
        Field field = ReflectionAccess.findField(ownerClass, fieldName, true);
        if (field == null) {
            LOGGER.warn("Failed to locate NeoForge internal field {} on {}", fieldName, ownerClass.getName());
            return false;
        }
        int modifiers = field.getModifiers();
        if (field.getDeclaringClass().isRecord()) {
            LOGGER.warn("Refusing to mutate record field {}.{}", field.getDeclaringClass().getName(), field.getName());
            return false;
        }
        if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
            LOGGER.warn("Refusing to mutate static final NeoForge internal field {}.{}", field.getDeclaringClass().getName(), field.getName());
            return false;
        }
        return ReflectionAccess.setFieldValue(field, instance, value);
    }
}
