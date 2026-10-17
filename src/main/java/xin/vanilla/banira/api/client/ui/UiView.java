package xin.vanilla.banira.api.client.ui;

import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.data.Component;
import java.util.function.Consumer;

public interface UiView {
    void text(String id, Component text);
    void value(String id, UiValue value);
    void enabled(String id, boolean enabled);
    AutoCloseable onAction(String id, Runnable action);
    AutoCloseable onValue(String id, Consumer<UiValue> action);
    void rows(String id, UiRowProvider rows, int maxRealizedRows);
    ScreenCoordinate bounds(String id);
}
