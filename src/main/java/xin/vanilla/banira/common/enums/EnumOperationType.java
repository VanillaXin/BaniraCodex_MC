package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

public enum EnumOperationType implements IEnumDescribable {
    ADD,
    SET,
    REMOVE,
    DEL,
    LIST,
    GET,
    CLEAR,
    ;

    public static EnumOperationType valueOfEx(Object type) {
        if (type instanceof EnumOperationType) return (EnumOperationType) type;
        if (type instanceof String) {
            for (EnumOperationType value : values()) {
                String str = (String) type;
                if (value.name().equalsIgnoreCase(str) || value.name().equals(str)) {
                    return value;
                }
            }
        } else if (type instanceof Number) {
            int i = ((Number) type).intValue();
            if (i >= 0 && i < values().length) return values()[i];
        }
        return null;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
