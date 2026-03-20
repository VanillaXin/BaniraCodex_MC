package xin.vanilla.banira.client.gui.quickaction;

import xin.vanilla.banira.common.enums.EnumPosition;

/**
 * 将 {@link EnumPosition} 映射为矩形内锚点相对左上角的偏移
 */
public final class QuickActionAnchorMath {
    private QuickActionAnchorMath() {
    }

    /**
     * @param out 长度 2，写入 [offsetX, offsetY]
     */
    public static void offsetFromTopLeft(EnumPosition anchor, double width, double height, double[] out) {
        if (anchor == null) {
            anchor = EnumPosition.TOP_LEFT;
        }
        switch (anchor) {
            case TOP_LEFT:
                out[0] = 0;
                out[1] = 0;
                break;
            case TOP_CENTER:
                out[0] = width * 0.5;
                out[1] = 0;
                break;
            case TOP_RIGHT:
                out[0] = width;
                out[1] = 0;
                break;
            case LEFT_CENTER:
                out[0] = 0;
                out[1] = height * 0.5;
                break;
            case CENTER:
                out[0] = width * 0.5;
                out[1] = height * 0.5;
                break;
            case RIGHT_CENTER:
                out[0] = width;
                out[1] = height * 0.5;
                break;
            case BOTTOM_LEFT:
                out[0] = 0;
                out[1] = height;
                break;
            case BOTTOM_CENTER:
                out[0] = width * 0.5;
                out[1] = height;
                break;
            case BOTTOM_RIGHT:
                out[0] = width;
                out[1] = height;
                break;
            default:
                out[0] = 0;
                out[1] = 0;
                break;
        }
    }
}
