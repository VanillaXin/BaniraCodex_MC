package xin.vanilla.banira.common.config.view;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Defaults are lazy and local to one unbound lifecycle, never a global mutable bean.
 */
final class ConfigViewDefaults {
    private final Map<Class<?>, Object> owners = new IdentityHashMap<>();

    synchronized Object read(ConfigViewField<?> field) {
        Class<?> owner = field.declaration.getDeclaringClass();
        try {
            Object bean = owners.get(owner);
            if (bean == null) {
                Constructor<?> constructor = owner.getDeclaredConstructor();
                constructor.setAccessible(true);
                bean = constructor.newInstance();
                owners.put(owner, bean);
            }
            return field.declaration.get(bean);
        } catch (ReflectiveOperationException | LinkageError e) {
            Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
            throw new IllegalStateException("Cannot initialize config default: " + field.path, cause);
        }
    }

    synchronized void clear() {
        owners.clear();
    }
}
