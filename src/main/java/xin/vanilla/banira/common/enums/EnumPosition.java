package xin.vanilla.banira.common.enums;

/**
 * 相对位置
 */
public enum EnumPosition {
    TOP_LEFT,
    TOP_RIGHT,
    TOP_CENTER,
    LEFT_CENTER,
    RIGHT_CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_CENTER,
    CENTER,
    ;

    public boolean isTop() {
        return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
    }

    public boolean isBottom() {
        return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
    }

    public boolean stacksDown() {
        return isTop() || this == LEFT_CENTER || this == RIGHT_CENTER || this == CENTER;
    }

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
