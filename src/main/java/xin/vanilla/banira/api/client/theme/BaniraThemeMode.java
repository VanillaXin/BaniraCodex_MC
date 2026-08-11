package xin.vanilla.banira.api.client.theme;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 子 Mod 使用 Banira 界面时可选择的主题模式。
 */
public enum BaniraThemeMode implements IEnumDescribable {
    FOLLOW_BANIRA,
    AUTO,
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
