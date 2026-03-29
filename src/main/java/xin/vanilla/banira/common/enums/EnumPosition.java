package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 相对位置
 */
public enum EnumPosition implements IEnumDescribable {
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
        if (obj instanceof EnumPosition position) return position;
        if (obj instanceof String s) {
            for (EnumPosition value : values()) {
                if (value.name().equalsIgnoreCase(s)) {
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

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
