package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

public enum EnumI18nType implements IEnumDescribable {
    NONE,
    /**
     * 无参文本
     */
    WORD,
    /**
     * 有参文本
     */
    FORMAT,
    /**
     * 纯文本
     */
    PLAIN,
    /**
     * 原始对象
     */
    ORIGINAL,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
