package xin.vanilla.banira.client.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 省略号位置
 */
public enum EnumEllipsisPosition implements IEnumDescribable {
    /**
     * 不显示省略号
     */
    NONE,
    /**
     * 文本开头
     */
    START,
    /**
     * 文本中间
     */
    MIDDLE,
    /**
     * 文本结尾
     */
    END,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
