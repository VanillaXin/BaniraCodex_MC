package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

public enum EnumNotificationHudMode implements IEnumDescribable {
    HOLD, TOGGLE, ALWAYS, OFF;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
