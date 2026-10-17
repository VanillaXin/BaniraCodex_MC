package xin.vanilla.banira.api.client.ui;

public interface UiPageSession {
    void bind(UiView view);
    void detach(UiDetachReason reason);
    void close(UiCloseReason reason);
}
