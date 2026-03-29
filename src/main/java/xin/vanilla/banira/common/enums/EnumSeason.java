package xin.vanilla.banira.common.enums;

import lombok.Getter;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 季节枚举
 */
@Getter
public enum EnumSeason implements IEnumDescribable {
    SPRING("春", "Spring"),
    SUMMER("夏", "Summer"),
    AUTUMN("秋", "Autumn"),
    WINTER("冬", "Winter"),
    AUTO("自动", "Auto"),
    ;

    private final String chineseName;
    private final String englishName;

    EnumSeason(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    public static EnumSeason valueOfEx(Object obj) {
        if (obj instanceof EnumSeason) return (EnumSeason) obj;
        if (obj instanceof String) {
            for (EnumSeason value : values()) {
                String str = (String) obj;
                if (value.name().equalsIgnoreCase(str) || value.getChineseName().equals(str) || value.getEnglishName().equals(str)) {
                    return value;
                }
            }
        } else if (obj instanceof Number) {
            int i = ((Number) obj).intValue();
            if (i >= 0 && i < values().length) return values()[i];
        }
        return null;
    }

    public static EnumSeason valueOfDefault(Object obj) {
        EnumSeason value = valueOfEx(obj);
        return value == null ? AUTO : value;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
