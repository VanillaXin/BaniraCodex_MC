package xin.vanilla.banira.client.enums;

import lombok.Getter;

/**
 * 渲染深度
 */
@Getter
public enum EnumRenderDepth {
    /**
     * 背景
     */
    BACKGROUND(1),
    /**
     * 前景
     */
    FOREGROUND(250),
    /**
     * 悬浮窗
     */
    OVERLAY(500),
    /**
     * 提示
     */
    TOOLTIP(750),
    /**
     * 弹出提示
     */
    POPUP_TIPS(900),
    /**
     * 鼠标
     */
    MOUSE(1000);

    private final int depth;

    EnumRenderDepth(int depth) {
        this.depth = depth;
    }
}
