package xin.vanilla.banira.client.enums;

/**
 * 相对位置
 */
public enum EnumPosition {
    TOP_LEFT,
    TOP_RIGHT,
    TOP_CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_CENTER,
    CENTER,
    ;

    public static EnumPosition valueOfEx(Object obj) {
        if (obj instanceof EnumPosition) return (EnumPosition) obj;
        if (obj instanceof String) {
            for (EnumPosition value : values()) {
                if (value.name().equalsIgnoreCase((String) obj)) {
                    return value;
                }
            }
        }
        return null;
    }

    public static EnumPosition valueOfDefault(Object obj) {
        EnumPosition value = valueOfEx(obj);
        return value == null ? TOP_LEFT : value;
    }

    public static boolean isValid(Object obj) {
        return valueOfEx(obj) != null;
    }

}
