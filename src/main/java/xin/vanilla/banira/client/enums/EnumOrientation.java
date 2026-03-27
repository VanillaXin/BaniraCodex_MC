package xin.vanilla.banira.client.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 组件方向
 */
public enum EnumOrientation implements IEnumDescribable {
    /**
     * 垂直方向
     */
    VERTICAL,
    /**
     * 水平方向
     */
    HORIZONTAL,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
