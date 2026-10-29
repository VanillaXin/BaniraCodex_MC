package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiPageSpec;

public interface UiNativePage extends xin.vanilla.banira.api.client.ui.UiPageSource {
    String uiSurfaceId();

    UiPageSpec uiPage();

    default xin.vanilla.banira.api.client.ui.UiPageSession uiSession() {
        return null;
    }

    default void captureUiState(UiViewState state) {
    }

    default void prepareUiState(UiViewState state) {
    }

    @Override
    default xin.vanilla.banira.api.client.ui.UiPageSession pageSession() {
        return uiSession();
    }

    @Override
    default void capturePageState(xin.vanilla.banira.api.client.ui.UiPageState state) {
        if (state instanceof UiViewState) captureUiState((UiViewState) state);
    }

    @Override
    default void preparePageState(xin.vanilla.banira.api.client.ui.UiPageState state) {
        if (state instanceof UiViewState) prepareUiState((UiViewState) state);
    }
}
