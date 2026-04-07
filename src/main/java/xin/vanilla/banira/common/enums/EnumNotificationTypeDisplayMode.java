package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 客户端收到网络通知后，按类型的展示方式
 */
public enum EnumNotificationTypeDisplayMode implements IEnumDescribable {

    /**
     * Banira屏幕通知
     */
    OVERLAY,
    /**
     * 原版聊天栏
     */
    VANILLA_CHAT,
    /**
     * 原版操作栏
     */
    ACTION_BAR,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }

    public static EnumNotificationTypeDisplayMode parseOrDefault(String name) {
        if (name == null || name.isEmpty()) {
            return OVERLAY;
        }
        try {
            return EnumNotificationTypeDisplayMode.valueOf(name);
        } catch (Exception ignored) {
            return OVERLAY;
        }
    }
}
