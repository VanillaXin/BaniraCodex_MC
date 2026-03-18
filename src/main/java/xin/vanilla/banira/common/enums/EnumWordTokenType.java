package xin.vanilla.banira.common.enums;

import lombok.Getter;

/**
 * 词法分析器枚举
 */
@Getter
public enum EnumWordTokenType {
    /**
     * ASCII字母、下划线
     */
    LETTER(0),
    /**
     * 数字、小数点
     */
    NUMBER(1),
    /**
     * 标点
     */
    PUNCTUATION(2),
    /**
     * CJK等非ASCII字母
     */
    CJK(3),
    /**
     * 其他
     */
    OTHER(4),
    ;

    private final int value;

    EnumWordTokenType(int value) {
        this.value = value;
    }
}
