package xin.vanilla.banira.internal.client.compat.jei;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public final class JeiRuntimeAccess {
    private static final String INPUT_API = "mezz.jei.api.gui.inputs.IJeiUserInput";
    private static final ClassValue<Optional<Method>> CONFIG_GETTERS = new ClassValue<Optional<Method>>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            Method getter = publicMethod(type, "lookupHistoryEnabled");
            if (getter == null) getter = publicMethod(type, "isLookupHistoryEnabled");
            return Optional.ofNullable(getter);
        }
    };
    private static final ClassValue<Optional<Method>> VALUE_GETTERS = new ClassValue<Optional<Method>>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            return Optional.ofNullable(publicMethod(type, "get"));
        }
    };
    private static final ClassValue<Optional<InputFactory>> INPUT_FACTORIES = new ClassValue<Optional<InputFactory>>() {
        @Override protected Optional<InputFactory> computeValue(Class<?> input) {
            try {
                for (Method factory : input.getMethods()) {
                    Class<?>[] parameters = factory.getParameterTypes();
                    if (!factory.getName().equals("fromVanilla") || !Modifier.isStatic(factory.getModifiers())
                            || factory.getReturnType() != Optional.class || parameters.length != 4
                            || parameters[0] != double.class || parameters[1] != double.class
                            || parameters[2] != int.class || !parameters[3].isEnum()) continue;
                    Object execute = parameters[3].getField("EXECUTE").get(null);
                    return Optional.of(new InputFactory(factory, execute));
                }
                return Optional.empty();
            } catch (ReflectiveOperationException | LinkageError exception) {
                return Optional.empty();
            }
        }
    };

    private JeiRuntimeAccess() {
    }

    public static boolean supportsConfig(Class<?> type) {
        Optional<Method> getter = CONFIG_GETTERS.get(type);
        if (!getter.isPresent()) return false;
        Class<?> result = getter.get().getReturnType();
        return result == boolean.class || result == Boolean.class || publicMethod(result, "get") != null;
    }

    public static boolean lookupHistoryEnabled(Object config) {
        if (config == null) return false;
        try {
            Optional<Method> getter = CONFIG_GETTERS.get(config.getClass());
            if (!getter.isPresent()) return false;
            Object value = getter.get().invoke(config);
            if (value instanceof Boolean) return Boolean.TRUE.equals(value);
            if (value == null) return false;
            Optional<Method> read = VALUE_GETTERS.get(value.getClass());
            return read.isPresent() && Boolean.TRUE.equals(read.get().invoke(value));
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    public static boolean supportsInput(ClassLoader loader) {
        return inputFactory(loader).isPresent();
    }

    public static Object mouseInput(ClassLoader loader) {
        Optional<InputFactory> input = inputFactory(loader);
        if (!input.isPresent()) return null;
        try {
            Object result = input.get().factory.invoke(null, 0.0D, 0.0D, 0, input.get().execute);
            Object value = result instanceof Optional<?> ? ((Optional<?>) result).orElse(null) : null;
            Class<?> api = Class.forName(INPUT_API, false, loader);
            return api.isInstance(value) ? value : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static Optional<InputFactory> inputFactory(ClassLoader loader) {
        for (String name : new String[]{"mezz.jei.common.input.UserInput", "mezz.jei.gui.input.UserInput"}) {
            try {
                Class<?> input = Class.forName(name, false, loader);
                Class<?> api = Class.forName(INPUT_API, false, loader);
                if (!api.isAssignableFrom(input)) continue;
                Optional<InputFactory> factory = INPUT_FACTORIES.get(input);
                if (factory.isPresent()) return factory;
            } catch (ClassNotFoundException | LinkageError exception) {
                // Try the other native input package.
            }
        }
        return Optional.empty();
    }

    private static Method publicMethod(Class<?> type, String name) {
        for (Class<?> contract : type.getInterfaces()) {
            Method method = publicMethod(contract, name);
            if (method != null) return method;
        }
        if (Modifier.isPublic(type.getModifiers())) {
            try {
                Method method = type.getMethod(name);
                if (!Modifier.isStatic(method.getModifiers())
                        && Modifier.isPublic(method.getDeclaringClass().getModifiers())) return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        Class<?> parent = type.getSuperclass();
        return parent == null ? null : publicMethod(parent, name);
    }

    private static final class InputFactory {
        final Method factory;
        final Object execute;
        InputFactory(Method factory, Object execute) {
            this.factory = factory;
            this.execute = execute;
        }
    }
}
