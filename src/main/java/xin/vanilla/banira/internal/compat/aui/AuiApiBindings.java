package xin.vanilla.banira.internal.compat.aui;

import org.objectweb.asm.Type;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuiApiBindings {
    private final Map<String, Executable> members;

    private AuiApiBindings(Map<String, Executable> members) {
        this.members = Collections.unmodifiableMap(new LinkedHashMap<>(members));
    }

    public static AuiApiBindings bind(ClassLoader loader, AuiCapabilityContract.Result capability)
            throws ReflectiveOperationException {
        if (!capability.compatible()) throw new IllegalStateException(capability.mismatch());
        Map<String, Executable> members = new LinkedHashMap<>();
        for (AuiCapabilityContract.Member member : AuiCapabilityContract.requiredMembers()) {
            Class<?> owner = Class.forName(member.owner.replace('/', '.'), false, loader);
            Type[] arguments = Type.getArgumentTypes(member.descriptor);
            Class<?>[] parameters = new Class<?>[arguments.length];
            for (int i = 0; i < arguments.length; i++) parameters[i] = resolve(arguments[i], loader);
            Executable executable;
            if (member.name.equals("<init>")) {
                executable = owner.getConstructor(parameters);
            } else {
                Method method = owner.getDeclaredMethod(member.name, parameters);
                if (!Type.getMethodDescriptor(method).equals(member.descriptor)) {
                    throw new NoSuchMethodException(member.key());
                }
                executable = method;
            }
            if (!Modifier.isPublic(executable.getModifiers())
                    || Modifier.isStatic(executable.getModifiers()) != member.isStatic) {
                throw new NoSuchMethodException(member.key());
            }
            members.put(member.key(), executable);
        }
        return new AuiApiBindings(members);
    }

    public Object invoke(String key, Object target, Object... arguments) {
        Executable member = members.get(key);
        if (member == null) throw new IllegalArgumentException("Unknown AUI operation: " + key);
        try {
            if (member instanceof Constructor<?>) return ((Constructor<?>) member).newInstance(arguments);
            return ((Method) member).invoke(target, arguments);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Error) throw (Error) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IllegalStateException("AUI operation failed: " + key, cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("AUI invocation failed: " + key, failure);
        }
    }

    private static Class<?> resolve(Type type, ClassLoader loader) throws ClassNotFoundException {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return boolean.class;
            case Type.INT:
                return int.class;
            case Type.DOUBLE:
                return double.class;
            case Type.LONG:
                return long.class;
            case Type.FLOAT:
                return float.class;
            case Type.BYTE:
                return byte.class;
            case Type.CHAR:
                return char.class;
            case Type.SHORT:
                return short.class;
            case Type.VOID:
                return void.class;
            default:
                return Class.forName(type.getClassName(), false, loader);
        }
    }
}
