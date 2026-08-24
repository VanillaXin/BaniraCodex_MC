package xin.vanilla.banira.client.gui;

import xin.vanilla.banira.client.data.ScreenCoordinate;

/**
 * 计算玩家配置页底栏的等宽双按钮布局。
 */
final class PlayerConfigActionBarLayout {

    private PlayerConfigActionBarLayout() {
    }

    static ScreenCoordinate[] equalSplitButtons(int cardX, int cardY, int cardW, int cardH,
                                                int cardInner, int buttonHeight, int cardGap) {
        int buttonAreaHeight = buttonHeight + cardInner;
        int buttonY = cardY + cardH - buttonAreaHeight
                + (buttonAreaHeight - buttonHeight) / 2;
        int contentTotal = cardW - cardInner * 2 - cardGap;
        int zoneWidth = contentTotal / 2;
        int leftSegmentWidth = cardInner + zoneWidth;
        int rightSegmentX = cardX + leftSegmentWidth + cardGap;
        int rightSegmentWidth = cardW - leftSegmentWidth - cardGap;

        int leftX = cardX + (leftSegmentWidth - zoneWidth) / 2;
        int rightX = rightSegmentX + (rightSegmentWidth - zoneWidth) / 2;
        return new ScreenCoordinate[]{
                new ScreenCoordinate(leftX, buttonY, zoneWidth, buttonHeight),
                new ScreenCoordinate(rightX, buttonY, zoneWidth, buttonHeight)
        };
    }
}
