package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 移动方式
 */
public enum EnumMoveType implements IEnumDescribable {
    AUTO,
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP,
    FADE_IN,
    SCALE_AND_FADE,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
