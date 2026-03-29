package xin.vanilla.banira.client.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 悬浮提示纹理绘制模式
 */
public enum EnumTooltipTextureMode implements IEnumDescribable {
    /**
     * 跟随主题配置
     */
    AUTO,
    /**
     * 强制使用纹理
     */
    TEXTURE,
    /**
     * 强制使用颜色
     */
    COLOR,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
