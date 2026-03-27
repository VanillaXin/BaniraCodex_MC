package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 当对端客户端未安装 Banira、无法展示自定义 Notification 时，改用的原版展示方式。
 */
public enum EnumNotificationVanillaFallback implements IEnumDescribable {

    /**
     * 聊天栏消息（默认）
     */
    CHAT,
    /**
     * 操作栏（热键栏上方）
     */
    ACTION_BAR,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
