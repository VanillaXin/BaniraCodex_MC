package xin.vanilla.banira.internal.client.ui;

import java.util.Objects;

public final class UiViewState {
    private String scrollAnchor = "";
    private String focusedField = "";
    private String selectedId = "";
    private int selectionStart;
    private int selectionEnd;
    private double scrollOffset;
    private double rowPosition;
    private double detailScroll;
    public double detailScroll() { return detailScroll; }
    public void detailScroll(double value) { detailScroll = Double.isFinite(value) ? Math.max(0, value) : 0; }
    public String scrollAnchor() { return scrollAnchor; }
    public void scrollAnchor(String id) { scrollAnchor = Objects.requireNonNull(id, "id"); }
    public String focusedField() { return focusedField; }
    public void focusedField(String id) { focusedField = Objects.requireNonNull(id, "id"); }
    public String selectedId() { return selectedId; }
    public void selectedId(String id) { selectedId = Objects.requireNonNull(id, "id"); }
    public int selectionStart() { return selectionStart; }
    public int selectionEnd() { return selectionEnd; }
    public void selection(String id, int start, int end) {
        focusedField(id);
        selectionStart = Math.max(0, start);
        selectionEnd = Math.max(0, end);
    }
    public double scrollOffset() { return scrollOffset; }
    public void scrollOffset(double value) { scrollOffset = Double.isFinite(value) ? Math.max(0, value) : 0; }
    public double rowPosition() { return rowPosition; }
    public void rowPosition(double value) { rowPosition = Double.isFinite(value) ? Math.max(0, value) : 0; }
}
