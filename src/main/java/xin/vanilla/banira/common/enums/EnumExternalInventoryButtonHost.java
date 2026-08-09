package xin.vanilla.banira.common.enums;

import lombok.Getter;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/** 决定已适配的第三方背包按钮由哪个界面统一展示。 */
@Getter
public enum EnumExternalInventoryButtonHost implements IEnumDescribable {
    BANIRA("由 Banira 统一显示", "Show in Banira"),
    FTB_LIBRARY("由 FTB Library 统一显示", "Show in FTB Library"),
    ORIGINAL("保留各模组原始按钮", "Keep original buttons"),
    ;

    private final String chineseName;
    private final String englishName;

    EnumExternalInventoryButtonHost(String chineseName, String englishName) {
        this.chineseName = chineseName;
        this.englishName = englishName;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
