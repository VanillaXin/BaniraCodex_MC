package xin.vanilla.banira.client.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 排列方式
 */
public enum EnumAlignment implements IEnumDescribable {
    /**
     * 前对齐
     */
    START,
    /**
     * 居中对齐
     */
    CENTER,
    /**
     * 后对齐
     */
    END,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
