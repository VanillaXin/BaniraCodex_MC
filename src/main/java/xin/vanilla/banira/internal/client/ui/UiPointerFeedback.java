package xin.vanilla.banira.internal.client.ui;

public interface UiPointerFeedback {
    void pointerButton(int button, boolean down);

    void pointerScroll(double delta);
}
