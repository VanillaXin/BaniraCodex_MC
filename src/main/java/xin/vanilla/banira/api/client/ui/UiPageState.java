package xin.vanilla.banira.api.client.ui;

import java.util.Objects;

public class UiPageState {
    private String scrollAnchor = "", focusedField = "", selectedId = "";
    private int selectionStart, selectionEnd;
    private double scrollOffset, rowPosition, detailScroll;
    public String scrollAnchor() { return scrollAnchor; }
    public void scrollAnchor(String value) { scrollAnchor = Objects.requireNonNull(value); }
    public String focusedField() { return focusedField; }
    public void focusedField(String value) { focusedField = Objects.requireNonNull(value); }
    public String selectedId() { return selectedId; }
    public void selectedId(String value) { selectedId = Objects.requireNonNull(value); }
    public int selectionStart() { return selectionStart; }
    public int selectionEnd() { return selectionEnd; }
    public void selection(String id, int start, int end) {
        focusedField(id); selectionStart = Math.max(0, start); selectionEnd = Math.max(0, end);
    }
    public double scrollOffset() { return scrollOffset; }
    public void scrollOffset(double value) { scrollOffset = finite(value); }
    public double rowPosition() { return rowPosition; }
    public void rowPosition(double value) { rowPosition = finite(value); }
    public double detailScroll() { return detailScroll; }
    public void detailScroll(double value) { detailScroll = finite(value); }
    private static double finite(double value) { return Double.isFinite(value) ? Math.max(0, value) : 0; }
}
