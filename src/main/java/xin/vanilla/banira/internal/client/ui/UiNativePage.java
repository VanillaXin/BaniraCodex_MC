package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiPageSpec;

public interface UiNativePage {
    String uiSurfaceId();
    UiPageSpec uiPage();
    default xin.vanilla.banira.api.client.ui.UiPageSession uiSession() { return null; }
    default void captureUiState(UiViewState state) { }
    default void prepareUiState(UiViewState state) { }
}
