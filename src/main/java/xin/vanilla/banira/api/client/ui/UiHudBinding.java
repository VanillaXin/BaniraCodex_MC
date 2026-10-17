package xin.vanilla.banira.api.client.ui;

public interface UiHudBinding {
    boolean visible();
    void bind(UiView view);
    void detach(UiDetachReason reason);
}
