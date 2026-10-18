package xin.vanilla.banira.api.client.ui;

import xin.vanilla.banira.client.data.BaniraColorConfig;

import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.data.Component;
import java.util.function.Consumer;

public interface UiView {
    void theme(BaniraColorConfig theme);
    void text(String id, Component text);
    void tooltip(String id, Component text);
    void value(String id, UiValue value);
    void control(String id, UiControlSpec control);
    void draft(String id, String draft);
    void enabled(String id, boolean enabled);
    AutoCloseable onAction(String id, Runnable action);
    AutoCloseable onLongAction(String id, Runnable action);
    default AutoCloseable onLongAction(String id, long durationMs, Runnable action) { return onLongAction(id, action); }
    AutoCloseable onValue(String id, Consumer<UiValue> action);
    AutoCloseable onDraft(String id, Consumer<String> action);
    AutoCloseable onPointer(String id, Consumer<UiPointer> action);
    void hudPosition(double x, double y);
    default void gameElement(String id, UiGameElement element) { }
    default void hudFrame(UiHudFrame frame) { throw new UnsupportedOperationException("HUD frames unavailable"); }
    void rows(String id, UiRowProvider rows, int maxRealizedRows);
    ScreenCoordinate bounds(String id);
    void requestClose();
}
