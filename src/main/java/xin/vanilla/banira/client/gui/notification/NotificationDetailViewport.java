package xin.vanilla.banira.client.gui.notification;

public final class NotificationDetailViewport {
    private int x, y, width, height, lineHeight = 1, contentLines;
    private double scrollPixels;

    public void layout(int x, int y, int width, int height, int lineHeight, int contentLines) {
        if (lineHeight <= 0) throw new IllegalArgumentException("Line height must be positive");
        this.x = x; this.y = y;
        this.width = Math.max(0, width); this.height = Math.max(0, height);
        this.lineHeight = lineHeight; this.contentLines = Math.max(0, contentLines);
        scrollPixels = Math.min(scrollPixels, maxScrollPixels());
    }

    public void scrollBy(double rows) {
        if (Double.isFinite(rows)) scrollTo(scrollRows() + rows);
    }

    public void scrollTo(double rows) {
        if (Double.isFinite(rows)) scrollPixels = Math.floor(Math.max(0,
                Math.min(maxScrollPixels(), rows * lineHeight)));
    }

    public void reset() { scrollPixels = 0; }

    public boolean contains(double mouseX, double mouseY) {
        return width > 0 && height > 0 && mouseX >= x && mouseX < (double) x + width
                && mouseY >= y && mouseY < (double) y + height;
    }

    public int visibleLineAt(double mouseX, double mouseY) {
        if (!contains(mouseX, mouseY)) return -1;
        int index = (int) Math.floor((mouseY - y + scrollPixels) / lineHeight);
        return index >= 0 && index < contentLines ? index : -1;
    }

    public int lineY(int sourceLine) {
        return (int) Math.floor(y + (double) sourceLine * lineHeight - scrollPixels);
    }

    public int firstLine() { return (int) (scrollPixels / lineHeight); }
    public int lastLineExclusive() {
        return Math.min(contentLines, (int) Math.ceil((scrollPixels + height) / lineHeight));
    }
    public double scrollRows() { return scrollPixels / lineHeight; }
    public double maxScrollRows() { return maxScrollPixels() / lineHeight; }
    public double visibleRows() { return (double) height / lineHeight; }
    private double maxScrollPixels() { return Math.max(0, (double) contentLines * lineHeight - height); }
}
