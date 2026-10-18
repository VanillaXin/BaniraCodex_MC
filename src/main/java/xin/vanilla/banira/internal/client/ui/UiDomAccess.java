package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.common.data.Component;

import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.DropdownOption;

import java.util.function.Consumer;

public interface UiDomAccess {
    default void theme(BaniraColorConfig theme) { }
    default void hudPosition(double x, double y) { }
    Node find(String id);
    Node create(String tag);
    Node overlayRoot();
    boolean current();
    AutoCloseable color(Node anchor, String initial, Consumer<String> result);

    interface Event {
        int keyCode();
        default int button() { return -1; }
        default double x() { return Double.NaN; }
        default double y() { return Double.NaN; }
        void consume();
    }

    interface Node {
        void component(Component component);
        void text(String text);
        String value();
        void value(String value);
        boolean checked();
        void checked(boolean checked);
        void attribute(String name, String value);
        void removeAttribute(String name);
        void append(Node node);
        void remove();
        ScreenCoordinate bounds();
        double scrollTop();
        void scrollTop(double value);
        double clientHeight();
        boolean focused();
        void focus();
        void blur();
        int selectionStart();
        int selectionEnd();
        void selection(int start, int end);
        void tooltip(String text);
        void icon(DropdownOption option);
        AutoCloseable on(String event, Consumer<Event> action);
    }
}
