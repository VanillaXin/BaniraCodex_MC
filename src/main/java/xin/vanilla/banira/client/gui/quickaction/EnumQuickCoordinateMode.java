package xin.vanilla.banira.client.gui.quickaction;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 图标组锚点在XY轴上的坐标系
 */
public enum EnumQuickCoordinateMode implements IEnumDescribable {
    /**
     * 屏幕比例
     */
    RELATIVE,
    /**
     * 像素坐标
     */
    ABSOLUTE,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
