package xin.vanilla.banira.common.enums;

import lombok.Getter;

/**
 * 季节枚举
 */
@Getter
public enum EnumSeason {
    SPRING("春", "Spring"),
    SUMMER("夏", "Summer"),
    AUTUMN("秋", "Autumn"),
    WINTER("冬", "Winter");

    private final String chineseName;
    private final String englishName;

    EnumSeason(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }
}
