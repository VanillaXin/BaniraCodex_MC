package xin.vanilla.banira.internal.compat.aui;

import xin.vanilla.banira.client.data.ScreenCoordinate;

public final class AuiTransientLayout {
    private AuiTransientLayout() {
    }

    public static ScreenCoordinate tooltip(String text, double x, double y, double width, double height) {
        if (!finite(x, y, width, height) || width <= 8 || height <= 8) return new ScreenCoordinate(0, 0, 0, 0);
        double longest = 0, line = 0;
        for (int offset = 0; offset < text.length(); ) {
            int point = text.codePointAt(offset);
            offset += Character.charCount(point);
            if (point == '\n') {
                longest = Math.max(longest, line);
                line = 0;
            } else line += point >= 0x2e80 ? 9 : 5.5;
        }
        longest = Math.max(longest, line);
        double w = Math.min(width - 8, Math.min(230, Math.max(38, longest + 16)));
        int lines = 0;
        for (String paragraph : text.split("\n", -1)) {
            double extent = paragraph.codePoints().mapToDouble(point -> point >= 0x2e80 ? 9 : 5.5).sum();
            lines += Math.max(1, (int) Math.ceil(extent / Math.max(1, w - 16)));
        }
        double h = Math.min(height - 8, lines * 11.0 + 10);
        double left = x + 10, top = y + 12;
        if (left + w > width - 4) left = x - w - 8;
        if (top + h > height - 4) top = y - h - 8;
        return new ScreenCoordinate(clamp(left, 4, width - w - 4), clamp(top, 4, height - h - 4), w, h);
    }

    public record ImeArea(int x, int y, int left, int top, int right, int bottom) {
    }

    public static ImeArea ime(ScreenCoordinate bounds, int guiWidth, int guiHeight, int clientWidth, int clientHeight) {
        if (bounds == null || !finite(bounds.x(), bounds.y(), bounds.width(), bounds.height())
                || bounds.width() <= 0 || bounds.height() <= 0 || guiWidth <= 0 || guiHeight <= 0 || clientWidth <= 0 || clientHeight <= 0)
            return null;
        double sx = (double) clientWidth / guiWidth, sy = (double) clientHeight / guiHeight;
        int left = (int) clamp(Math.floor(bounds.x() * sx), 0, clientWidth - 1);
        int top = (int) clamp(Math.floor(bounds.y() * sy), 0, clientHeight - 1);
        int right = (int) clamp(Math.ceil((bounds.x() + bounds.width()) * sx), 0, clientWidth);
        int bottom = (int) clamp(Math.ceil((bounds.y() + bounds.height()) * sy), 0, clientHeight);
        if (right <= left || bottom <= top) return null;
        return new ImeArea((int) clamp(left + 4 * sx, left, right - 1), Math.min(clientHeight - 1, bottom), left, top, right, bottom);
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
