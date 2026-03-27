package xin.vanilla.banira.client.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 坐标类型
 */
public enum EnumCoordinateType implements IEnumDescribable {
    /**
     * 绝对坐标(仍然需要加上父坐标)
     */
    ABSOLUTE,
    /**
     * 相对坐标(百分比于父控件中)
     */
    RELATIVE_PERCENT,
    /**
     * 相对坐标(大于0则相对父控件左/上, 小于0则相对控件右/下)
     */
    RELATIVE_PIXEL,
    /**
     * 居中
     */
    CENTER,
    /**
     * 相对鼠标坐标
     */
    RELATIVE_MOUSE,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
