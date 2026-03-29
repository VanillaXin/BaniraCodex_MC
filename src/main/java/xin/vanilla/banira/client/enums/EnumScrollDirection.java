package xin.vanilla.banira.client.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 滚动条方向
 */
public enum EnumScrollDirection implements IEnumDescribable {
    /**
     * 垂直滚动
     */
    VERTICAL,
    /**
     * 横向滚动
     */
    HORIZONTAL,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
