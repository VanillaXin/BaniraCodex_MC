package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.Gson;

import java.util.Arrays;

/**
 * A versioned local-only constructor selection stored in the existing step value.
 */
final class QuickActionScreenTarget {
    private static final String PREFIX = "@banira-screen-v1:";
    String className;
    String[] types;
    String[] arguments;

    QuickActionScreenTarget(String className, String[] types, String[] arguments) {
        this.className = className;
        this.types = types.clone();
        this.arguments = arguments.clone();
        validate();
    }

    String encode() {
        validate();
        String encoded = PREFIX + new Gson().toJson(this);
        if (encoded.length() > 16384) throw new IllegalArgumentException("Screen arguments are too long");
        return encoded;
    }

    static QuickActionScreenTarget decode(String value) {
        if (value == null || !value.startsWith(PREFIX)) return null;
        if (value.length() > 16384) throw new IllegalArgumentException("Screen arguments are too long");
        QuickActionScreenTarget target = new Gson().fromJson(value.substring(PREFIX.length()), QuickActionScreenTarget.class);
        if (target == null) throw new IllegalArgumentException("Missing screen target");
        target.validate();
        return target;
    }

    static String summary(String value) {
        try {
            QuickActionScreenTarget target = decode(value);
            return target == null ? value : target.className + "(" + String.join(", ", target.arguments) + ")";
        } catch (RuntimeException exception) {
            return value;
        }
    }

    private void validate() {
        if (className == null || className.isEmpty() || className.length() > 512
                || types == null || arguments == null || types.length != arguments.length || types.length > 32)
            throw new IllegalArgumentException("Invalid screen constructor");
        for (int i = 0; i < types.length; i++) parse(types[i], arguments[i]);
    }

    Class<?>[] parameterTypes() {
        return Arrays.stream(types).map(QuickActionScreenTarget::type).toArray(Class<?>[]::new);
    }

    Object[] values() {
        Object[] result = new Object[types.length];
        for (int i = 0; i < types.length; i++) result[i] = parse(types[i], arguments[i]);
        return result;
    }

    static boolean supported(String name) {
        try {
            type(name);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static Class<?> type(String name) {
        if (name == null) throw new IllegalArgumentException("Missing parameter type");
        switch (name) {
            case "java.lang.String":
                return String.class;
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "char":
                return char.class;
            case "java.lang.Boolean":
                return Boolean.class;
            case "java.lang.Byte":
                return Byte.class;
            case "java.lang.Short":
                return Short.class;
            case "java.lang.Integer":
                return Integer.class;
            case "java.lang.Long":
                return Long.class;
            case "java.lang.Float":
                return Float.class;
            case "java.lang.Double":
                return Double.class;
            case "java.lang.Character":
                return Character.class;
            default:
                throw new IllegalArgumentException("Unsupported parameter type: " + name);
        }
    }

    static Object parse(String name, String value) {
        Class<?> type = type(name);
        if (value == null || value.length() > 1024) throw new IllegalArgumentException("Invalid argument length");
        if (type == String.class) return value;
        if (type == char.class || type == Character.class) {
            if (value.length() != 1) throw new IllegalArgumentException("Expected one character");
            return value.charAt(0);
        }
        value = value.trim();
        if (type == boolean.class || type == Boolean.class) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
                throw new IllegalArgumentException("Expected true or false");
            return Boolean.valueOf(value);
        }
        if (type == byte.class || type == Byte.class) return Byte.valueOf(value);
        if (type == short.class || type == Short.class) return Short.valueOf(value);
        if (type == int.class || type == Integer.class) return Integer.valueOf(value);
        if (type == long.class || type == Long.class) return Long.valueOf(value);
        if (type == float.class || type == Float.class) {
            float number = Float.parseFloat(value);
            if (!Float.isFinite(number)) throw new IllegalArgumentException("Expected a finite number");
            return number;
        }
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number)) throw new IllegalArgumentException("Expected a finite number");
        return number;
    }
}
