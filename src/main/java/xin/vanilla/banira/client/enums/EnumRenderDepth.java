package xin.vanilla.banira.client.enums;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;

/**
 * 渲染深度
 */
@Getter
@Accessors(fluent = true)
public enum EnumRenderDepth implements IEnumDescribable {
    /**
     * 背景
     */
    BACKGROUND(0),
    /**
     * 默认组件
     */
    DEFAULT(1),
    /**
     * 默认组件（卡片、面板等）
     */
    DEFAULT_ELEVATED(100),
    /**
     * 前景
     */
    FOREGROUND(250),
    /**
     * 悬浮窗（下拉、弹窗等）
     */
    OVERLAY(400),
    /**
     * 通知
     */
    NOTIFICATION(600),
    /**
     * 悬浮提示（Tooltip、Popup 菜单等）
     */
    TOOLTIP(800),
    /**
     * 鼠标光标
     */
    MOUSE(1000);

    private final int depth;

    EnumRenderDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(BaniraComponent.get(), this);
    }
}
