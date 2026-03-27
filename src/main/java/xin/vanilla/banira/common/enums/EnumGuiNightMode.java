package xin.vanilla.banira.common.enums;

import lombok.Getter;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * GUI 主题夜间模式（与昼夜配色联动，由客户端配置控制）
 */
@Getter
public enum EnumGuiNightMode implements IEnumDescribable {
    OFF("关闭", "Off"),
    ALWAYS("总是夜晚", "Always night"),
    SCHEDULED("指定时间段", "Scheduled"),
    AUTO("自动判断", "Auto"),
    ;

    private final String chineseName;
    private final String englishName;

    EnumGuiNightMode(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    public static EnumGuiNightMode valueOfEx(Object obj) {
        if (obj instanceof EnumGuiNightMode mode) {
            return mode;
        }
        if (obj instanceof String str) {
            for (EnumGuiNightMode value : values()) {
                if (value.name().equalsIgnoreCase(str)
                        || value.getChineseName().equals(str)
                        || value.getEnglishName().equalsIgnoreCase(str)) {
                    return value;
                }
            }
        } else if (obj instanceof Number n) {
            int i = n.intValue();
            if (i >= 0 && i < values().length) {
                return values()[i];
            }
        }
        return null;
    }

    public static EnumGuiNightMode valueOfDefault(Object obj) {
        EnumGuiNightMode v = valueOfEx(obj);
        return v == null ? OFF : v;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
