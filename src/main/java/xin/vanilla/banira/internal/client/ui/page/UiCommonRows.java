package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.internal.client.ui.UiRowPresentation;

final class UiCommonRows implements UiRowPresentation {
    private final UiRowProvider rows;
    private final int height;
    private final int columns;
    private final String style;

    UiCommonRows(UiRowProvider rows, int height, String style) {
        this(rows, height, style, 1);
    }

    UiCommonRows(UiRowProvider rows, int height, String style, int columns) {
        this.rows = rows;
        this.height = height;
        this.style = style;
        this.columns = columns;
    }

    public int size() {
        return rows.size();
    }

    public String key(int index) {
        return rows.key(index);
    }

    public void bind(UiView row, int index) {
        rows.bind(row, index);
    }

    public int rowHeight() {
        return height;
    }

    public int columns() {
        return columns;
    }

    public boolean textTools() {
        return true;
    }

    public String rowClass(int index) {
        return style;
    }
}
