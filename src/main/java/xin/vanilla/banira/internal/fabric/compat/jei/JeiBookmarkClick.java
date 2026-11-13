package xin.vanilla.banira.internal.fabric.compat.jei;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * Resolves the input contract of the installed JEI version once per button.
 */
final class JeiBookmarkClick {
    private JeiBookmarkClick() {
    }

    static Runnable bind(Object button) throws ReflectiveOperationException {
        Method click = null;
        for (Method method : button.getClass().getDeclaredMethods()) {
            if (method.getName().equals("onMouseClicked") && method.getParameterCount() == 1
                    && method.getReturnType() == boolean.class) {
                click = method;
                break;
            }
        }
        if (click == null) throw new NoSuchMethodException("JEI bookmark onMouseClicked");
        Class<?> inputClass = click.getParameterTypes()[0];
        Method factory = null;
        Object execute = null;
        for (Method method : inputClass.getMethods()) {
            Class<?>[] args = method.getParameterTypes();
            if (method.getName().equals("fromVanilla") && Modifier.isStatic(method.getModifiers())
                    && (method.getReturnType() == inputClass || method.getReturnType() == Optional.class) && args.length == 4
                    && args[0] == double.class && args[1] == double.class && args[2] == int.class
                    && args[3].isEnum()) {
                for (Object constant : args[3].getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals("EXECUTE")) {
                        execute = constant;
                        factory = method;
                    }
                }
            }
        }
        if (factory == null) throw new NoSuchMethodException("JEI bookmark input factory");
        click.setAccessible(true);
        Method nativeClick = click;
        Object input = factory.invoke(null, 0.0, 0.0, 0, execute);
        if (input instanceof Optional<?>) input = ((Optional<?>) input).orElse(null);
        if (!inputClass.isInstance(input)
                || !Boolean.FALSE.equals(inputClass.getMethod("isSimulate").invoke(input)))
            throw new IllegalStateException("JEI mouse input unavailable");
        Object mouseInput = input;
        return () -> {
            try {
                nativeClick.invoke(button, mouseInput);
            } catch (InvocationTargetException exception) {
                throw new IllegalStateException("JEI bookmark click failed", exception.getCause());
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("JEI bookmark click failed", exception);
            }
        };
    }
}
