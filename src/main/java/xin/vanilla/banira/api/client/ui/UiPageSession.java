package xin.vanilla.banira.api.client.ui;

public interface UiPageSession {
    default void tick() { }
    void bind(UiView view);
    void detach(UiDetachReason reason);
    void close(UiCloseReason reason);
}
