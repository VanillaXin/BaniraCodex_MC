package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.ScreenCoordinate;

public final class TextCompletionLayout {
    private TextCompletionLayout() {
    }

    public static ScreenCoordinate bounds(ScreenCoordinate anchor, ScreenCoordinate viewport, int count, int rowHeight) {
        double edge = 4;
        double left = viewport.x() + edge, top = viewport.y() + edge;
        double right = viewport.x() + viewport.width() - edge, bottom = viewport.y() + viewport.height() - edge;
        double above = Math.max(0, anchor.y() - 2 - top);
        double below = Math.max(0, bottom - anchor.y() - anchor.height() - 2);
        boolean down = below >= above;
        double available = Math.max(above, below);
        int rows = Math.max(0, Math.min(Math.min(8, count), (int) Math.floor((available - 8) / rowHeight)));
        double height = rows == 0 ? 0 : rows * rowHeight + 8;
        double width = Math.max(0, Math.min(right - left, Math.max(160, anchor.width())));
        double x = Math.max(left, Math.min(anchor.x(), right - width));
        double y = down ? anchor.y() + anchor.height() + 2 : anchor.y() - height - 2;
        return new ScreenCoordinate(x, Math.max(top, Math.min(y, bottom - height)), width, height);
    }
}
