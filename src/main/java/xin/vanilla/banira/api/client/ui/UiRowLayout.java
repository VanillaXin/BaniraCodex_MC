package xin.vanilla.banira.api.client.ui;

/** Optional row geometry and styling shared by native/AUI page providers. */
public interface UiRowLayout extends UiRowProvider {
    int rowHeight();
    default String scrollerId() { return "content"; }
    default boolean tracksPageScroll() { return true; }
    default boolean textTools() { return false; }
    default String rowClass(int index) { return "row"; }
}
