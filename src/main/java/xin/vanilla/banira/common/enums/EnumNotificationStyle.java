package xin.vanilla.banira.common.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 通知语义样式
 */
public enum EnumNotificationStyle implements IEnumDescribable {

    /**
     * 普通
     */
    NORMAL,
    /**
     * 警告
     */
    WARNING,
    /**
     * 错误
     */
    ERROR,
    /**
     * 成功 / 积极反馈
     */
    SUCCESS,
    ;

    public static EnumNotificationStyle valueOfEx(String name) {
        if (name == null || name.isEmpty()) {
            return NORMAL;
        }
        try {
            return EnumNotificationStyle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(this);
    }
}
