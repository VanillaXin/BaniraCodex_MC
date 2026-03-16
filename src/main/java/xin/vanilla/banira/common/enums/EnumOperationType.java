package xin.vanilla.banira.common.enums;

public enum EnumOperationType {
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

    /**
     * 从字符串解析操作类型，无效时返回 null
     */
    public static EnumOperationType fromString(String str) {
        return valueOfEx(str);
    }
}
