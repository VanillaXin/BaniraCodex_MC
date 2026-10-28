package xin.vanilla.banira.api.client.ui;

public interface UiRowProvider {
    int size();

    String key(int index);

    void bind(UiView row, int index);
}
