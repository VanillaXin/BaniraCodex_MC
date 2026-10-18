package xin.vanilla.banira.api.client.ui;

public interface UiPageSource {
    String uiSurfaceId();
    UiPageSpec uiPage();
    default UiPageSession pageSession() { return null; }
    default void capturePageState(UiPageState state) { }
    default void preparePageState(UiPageState state) { }
}
