package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

public enum EnumNotificationHudHost implements IEnumDescribable {
    AUTO, STANDALONE, XAERO, JOURNEYMAP, FTB_CHUNKS, VOXELMAP, MAP_ATLASES;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
