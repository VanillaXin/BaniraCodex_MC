package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiRowProvider;

public interface UiRowPresentation extends UiRowProvider {
    int rowHeight();
    default String scrollerId() { return "content"; }
    default boolean tracksPageScroll() { return true; }
    default boolean textTools() { return false; }
    default String rowClass(int index) { return "row"; }
}
