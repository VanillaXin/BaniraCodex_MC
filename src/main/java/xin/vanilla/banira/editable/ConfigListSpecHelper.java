package xin.vanilla.banira.editable;

import me.shedaniel.autoconfig.annotation.ConfigEntry;
import xin.vanilla.banira.editable.annotation.BaniraFieldMeta;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 根据 {@code List<T>} 泛型与注解解析列表配置项类型，并规范化默认值与 GUI/网络表示。
 */
public final class ConfigListSpecHelper {

    private ConfigListSpecHelper() {
    }

    public static Class<?> resolveListElementClass(Field field) {
        java.lang.reflect.Type gen = field.getGenericType();
        if (gen instanceof ParameterizedType) {
            java.lang.reflect.Type[] args = ((ParameterizedType) gen).getActualTypeArguments();
            if (args.length > 0 && args[0] instanceof Class) {
                return (Class<?>) args[0];
            }
        }
        return String.class;
    }

    public static ConfigEntryDescriptor.ConfigValueType listValueTypeForElement(Class<?> elem) {
        if (elem == String.class) {
            return ConfigEntryDescriptor.ConfigValueType.STRING_LIST;
        }
        if (elem == int.class || elem == Integer.class) {
            return ConfigEntryDescriptor.ConfigValueType.INTEGER_LIST;
        }
        if (elem == long.class || elem == Long.class) {
            return ConfigEntryDescriptor.ConfigValueType.LONG_LIST;
        }
        if (elem == double.class || elem == Double.class || elem == float.class || elem == Float.class) {
            return ConfigEntryDescriptor.ConfigValueType.DOUBLE_LIST;
        }
        if (elem == boolean.class || elem == Boolean.class) {
            return ConfigEntryDescriptor.ConfigValueType.BOOLEAN_LIST;
        }
        if (elem.isEnum()) {
            return ConfigEntryDescriptor.ConfigValueType.ENUM_LIST;
        }
        throw new IllegalArgumentException("Unsupported List element type: " + elem.getName()
                + " for field (use String, boxed primitives, or enum)");
    }

    public static Number[] listBoundsForValueType(Field field, ConfigEntryDescriptor.ConfigValueType listType) {
        switch (listType) {
            case INTEGER_LIST: {
                ConfigEntry.BoundedDiscrete bd = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                if (bd != null) {
                    return new Number[]{bd.min(), bd.max()};
                }
                return new Number[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
            }
            case LONG_LIST: {
                BaniraFieldMeta.BoundedLong bl = field.getAnnotation(BaniraFieldMeta.BoundedLong.class);
                if (bl != null) {
                    return new Number[]{bl.min(), bl.max()};
                }
                return new Number[]{Long.MIN_VALUE, Long.MAX_VALUE};
            }
            case DOUBLE_LIST: {
                BaniraFieldMeta.BoundedDouble bd = field.getAnnotation(BaniraFieldMeta.BoundedDouble.class);
                if (bd != null) {
                    return new Number[]{bd.min(), bd.max()};
                }
                return new Number[]{Double.MIN_VALUE, Double.MAX_VALUE};
            }
            default:
                return new Number[]{null, null};
        }
    }

    public static int decimalPlacesForList(Field field, ConfigEntryDescriptor.ConfigValueType listType) {
        if (listType != ConfigEntryDescriptor.ConfigValueType.DOUBLE_LIST) {
            return 2;
        }
        BaniraFieldMeta.BoundedDouble bd = field.getAnnotation(BaniraFieldMeta.BoundedDouble.class);
        return bd != null ? bd.decimalPlaces() : 2;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends Enum<?>> enumClassForList(Class<?> elem) {
        if (!elem.isEnum()) {
            return null;
        }
        return (Class<? extends Enum<?>>) elem;
    }

    public static List<Object> normalizeDefaultList(List<?> raw, ConfigEntryDescriptor.ConfigValueType listType,
                                                    Class<? extends Enum<?>> enumClass, Number min, Number max,
                                                    int decimalPlaces) {
        if (raw == null) {
            return new ArrayList<>();
        }
        List<Object> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            Object one = coerceListElement(o, listType, enumClass, min, max, decimalPlaces);
            if (one != null) {
                out.add(one);
            }
        }
        return out;
    }

    public static Predicate<Object> listValidator(ConfigEntryDescriptor.ConfigValueType listType,
                                                  Class<? extends Enum<?>> enumClass, Number min, Number max,
                                                  int decimalPlaces) {
        switch (listType) {
            case STRING_LIST:
                return o -> o instanceof String;
            case INTEGER_LIST:
                return o -> {
                    Integer v = coerceToInteger(o);
                    return v != null && inIntegerRange(v, min, max);
                };
            case LONG_LIST:
                return o -> {
                    Long v = coerceToLong(o);
                    return v != null && inLongRange(v, min, max);
                };
            case DOUBLE_LIST:
                return o -> {
                    Double v = coerceToDouble(o);
                    return v != null && inDoubleRange(v, min, max, decimalPlaces);
                };
            case BOOLEAN_LIST:
                return o -> o instanceof Boolean
                        || (o instanceof String && isBooleanString((String) o));
            case ENUM_LIST:
                return o -> {
                    if (enumClass == null) {
                        return false;
                    }
                    if (o instanceof Enum && enumClass.isAssignableFrom(o.getClass())) {
                        return true;
                    }
                    if (o instanceof String) {
                        return parseEnum(enumClass, (String) o) != null;
                    }
                    return false;
                };
            default:
                return o -> false;
        }
    }

    public static Object coerceListElement(Object o, ConfigEntryDescriptor.ConfigValueType listType,
                                           Class<? extends Enum<?>> enumClass, Number min, Number max,
                                           int decimalPlaces) {
        if (o == null) {
            return null;
        }
        switch (listType) {
            case STRING_LIST:
                return String.valueOf(o);
            case INTEGER_LIST: {
                Integer v = coerceToInteger(o);
                if (v == null || !inIntegerRange(v, min, max)) {
                    return null;
                }
                return v;
            }
            case LONG_LIST: {
                Long v = coerceToLong(o);
                if (v == null || !inLongRange(v, min, max)) {
                    return null;
                }
                return v;
            }
            case DOUBLE_LIST: {
                Double v = coerceToDouble(o);
                if (v == null || !inDoubleRange(v, min, max, decimalPlaces)) {
                    return null;
                }
                return roundDouble(v, decimalPlaces);
            }
            case BOOLEAN_LIST:
                if (o instanceof Boolean) {
                    return o;
                }
                if (o instanceof String) {
                    String s = ((String) o).trim();
                    if ("true".equalsIgnoreCase(s)) {
                        return Boolean.TRUE;
                    }
                    if ("false".equalsIgnoreCase(s)) {
                        return Boolean.FALSE;
                    }
                }
                return null;
            case ENUM_LIST:
                if (enumClass == null) {
                    return null;
                }
                if (o instanceof Enum && enumClass.isAssignableFrom(o.getClass())) {
                    return o;
                }
                if (o instanceof String) {
                    return parseEnum(enumClass, (String) o);
                }
                return null;
            default:
                return null;
        }
    }

    private static boolean isBooleanString(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        return "true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t);
    }

    private static Integer coerceToInteger(Object o) {
        if (o instanceof Number) {
            long lv = ((Number) o).longValue();
            if (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE) {
                return (int) lv;
            }
            return null;
        }
        if (o instanceof String) {
            try {
                return Integer.parseInt(((String) o).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long coerceToLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double coerceToDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble(((String) o).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean inIntegerRange(int v, Number min, Number max) {
        if (min != null && v < min.intValue()) {
            return false;
        }
        return max == null || v <= max.intValue();
    }

    private static boolean inLongRange(long v, Number min, Number max) {
        if (min != null && v < min.longValue()) {
            return false;
        }
        return max == null || v <= max.longValue();
    }

    private static boolean inDoubleRange(double v, Number min, Number max, int decimalPlaces) {
        double x = roundDouble(v, decimalPlaces);
        if (min != null && x < min.doubleValue()) {
            return false;
        }
        return max == null || x <= max.doubleValue();
    }

    private static double roundDouble(double v, int decimalPlaces) {
        if (decimalPlaces < 0) {
            return v;
        }
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(v * factor) / factor;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> parseEnum(Class<? extends Enum<?>> enumClass, String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf((Class) enumClass, t);
        } catch (IllegalArgumentException ignored) {
            for (Enum<?> c : enumClass.getEnumConstants()) {
                if (c.name().equalsIgnoreCase(t)) {
                    return c;
                }
            }
            return null;
        }
    }

    public static List<?> normalizeListForRuntime(List<?> raw, ConfigEntryDescriptor desc) {
        if (raw == null) {
            return Collections.emptyList();
        }
        ConfigEntryDescriptor.ConfigValueType lt = desc.getValueType();
        Class<? extends Enum<?>> ec = desc.getEnumClass();
        Number min = desc.getMinValue();
        Number max = desc.getMaxValue();
        int dp = desc.getDecimalPlaces();
        if (lt == ConfigEntryDescriptor.ConfigValueType.STRING_LIST) {
            List<String> strs = new ArrayList<>(raw.size());
            for (Object o : raw) {
                Object c = coerceListElement(o, lt, ec, min, max, dp);
                if (c != null) {
                    strs.add((String) c);
                }
            }
            return strs;
        }
        List<Object> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            Object c = coerceListElement(o, lt, ec, min, max, dp);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    public static List<Object> normalizeListForGui(List<?> raw, ConfigEntryDescriptor desc) {
        if (raw == null) {
            return new ArrayList<>();
        }
        ConfigEntryDescriptor.ConfigValueType lt = desc.getValueType();
        Class<? extends Enum<?>> ec = desc.getEnumClass();
        Number min = desc.getMinValue();
        Number max = desc.getMaxValue();
        int dp = desc.getDecimalPlaces();
        List<Object> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            Object c = coerceListElement(o, lt, ec, min, max, dp);
            if (c != null) {
                out.add(toGuiItem(c, lt));
            }
        }
        return out;
    }

    private static Object toGuiItem(Object coerced, ConfigEntryDescriptor.ConfigValueType listType) {
        if (listType == ConfigEntryDescriptor.ConfigValueType.ENUM_LIST && coerced instanceof Enum) {
            return ((Enum<?>) coerced).name();
        }
        return coerced;
    }

    public static List<?> listFromGuiItems(List<Object> items, ConfigEntryDescriptor desc) {
        if (items == null) {
            return Collections.emptyList();
        }
        ConfigEntryDescriptor.ConfigValueType lt = desc.getValueType();
        Class<? extends Enum<?>> ec = desc.getEnumClass();
        Number min = desc.getMinValue();
        Number max = desc.getMaxValue();
        int dp = desc.getDecimalPlaces();
        if (lt == ConfigEntryDescriptor.ConfigValueType.STRING_LIST) {
            List<String> strs = new ArrayList<>(items.size());
            for (Object o : items) {
                Object coerced = coerceListElement(o, lt, ec, min, max, dp);
                if (coerced != null) {
                    strs.add((String) coerced);
                }
            }
            return strs;
        }
        List<Object> out = new ArrayList<>(items.size());
        for (Object o : items) {
            Object coerced = coerceListElement(o, lt, ec, min, max, dp);
            if (coerced != null) {
                out.add(coerced);
            }
        }
        return out;
    }

    /**
     * 解析网络同步用的逗号分隔字符串为列表（与 {@link xin.vanilla.banira.common.network.packet.ConfigSyncToServer#encodeConfigValue} 成对）。
     */
    public static List<?> parseNetworkCsv(String csv, ConfigEntryDescriptor desc) {
        if (csv == null || csv.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> parts = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        ConfigEntryDescriptor.ConfigValueType lt = desc.getValueType();
        Class<? extends Enum<?>> ec = desc.getEnumClass();
        Number min = desc.getMinValue();
        Number max = desc.getMaxValue();
        int dp = desc.getDecimalPlaces();
        List<Object> out = new ArrayList<>(parts.size());
        for (String t : parts) {
            Object raw;
            switch (lt) {
                case STRING_LIST:
                    raw = t;
                    break;
                case INTEGER_LIST:
                    raw = Integer.parseInt(t);
                    break;
                case LONG_LIST:
                    raw = Long.parseLong(t);
                    break;
                case DOUBLE_LIST:
                    raw = Double.parseDouble(t);
                    break;
                case BOOLEAN_LIST:
                    raw = Boolean.parseBoolean(t);
                    break;
                case ENUM_LIST:
                    raw = t;
                    break;
                default:
                    raw = t;
            }
            Object coerced = coerceListElement(raw, lt, ec, min, max, dp);
            if (coerced != null) {
                out.add(coerced);
            }
        }
        if (lt == ConfigEntryDescriptor.ConfigValueType.STRING_LIST) {
            List<String> strs = new ArrayList<>(out.size());
            for (Object o : out) {
                strs.add((String) o);
            }
            return strs;
        }
        return out;
    }
}
