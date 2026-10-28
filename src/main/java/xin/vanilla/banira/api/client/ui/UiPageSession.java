package xin.vanilla.banira.api.client.ui;

public interface UiPageSession {
    default boolean key(int keyCode, int scanCode, int modifiers, boolean pressed) {
        return false;
    }

    default void tick() {
    }

    void bind(UiView view);

    void detach(UiDetachReason reason);

    void close(UiCloseReason reason);
}
