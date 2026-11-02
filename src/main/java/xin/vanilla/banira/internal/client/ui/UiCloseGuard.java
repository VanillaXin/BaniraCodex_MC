package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiCloseReason;

public interface UiCloseGuard {
    boolean canClose(UiCloseReason reason);
}
