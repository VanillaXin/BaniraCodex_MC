package xin.vanilla.banira.client.gui.quickaction;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 快捷项展示方式
 */
public enum EnumQuickActionDisplay implements IEnumDescribable {
    /**
     * 在背包界面顶部图标组中显示
     */
    ICON,
    /**
     * 仅注册到列表
     */
    LIST_ONLY,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
