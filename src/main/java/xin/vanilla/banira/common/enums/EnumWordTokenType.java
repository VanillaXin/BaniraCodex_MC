package xin.vanilla.banira.common.enums;

import lombok.Getter;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 词法分析器枚举
 */
@Getter
public enum EnumWordTokenType implements IEnumDescribable {
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

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
