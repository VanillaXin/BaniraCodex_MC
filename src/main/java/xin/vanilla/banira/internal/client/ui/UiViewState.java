package xin.vanilla.banira.internal.client.ui;

import java.util.Objects;

public final class UiViewState {
    private String scrollAnchor = "";
    private String focusedField = "";
    private String selectedId = "";
    public String scrollAnchor() { return scrollAnchor; }
    public void scrollAnchor(String id) { scrollAnchor = Objects.requireNonNull(id, "id"); }
    public String focusedField() { return focusedField; }
    public void focusedField(String id) { focusedField = Objects.requireNonNull(id, "id"); }
    public String selectedId() { return selectedId; }
    public void selectedId(String id) { selectedId = Objects.requireNonNull(id, "id"); }
}
