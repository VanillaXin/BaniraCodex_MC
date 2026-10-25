package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiPageSession;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.gui.ConfirmDialogScreen;

import java.util.Objects;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;

public final class DialogUiSession implements UiPageSession {
    private final ConfirmDialogScreen.Args args;
    private boolean resolved;
    private UiOwnedResources bindings = new UiOwnedResources();

    public DialogUiSession(ConfirmDialogScreen.Args args) { this.args = Objects.requireNonNull(args, "args"); }

    public void confirm() { resolve(true); }
    public void cancel() { resolve(false); }
    public boolean resolved() { return resolved; }

    private void resolve(boolean confirm) {
        if (resolved) return;
        resolved = true;
        Runnable callback = confirm ? args.onConfirm() : args.onCancel();
        if (callback != null) callback.run();
    }

    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        bindings = new UiOwnedResources();
        view.text("title", args.title() != null ? args.title() : BaniraComponent.get().literal(""));
        UiCommonChrome.bind(view, args.title(), args.parentScreen());
        view.text("close", BaniraComponent.get().transClientAuto("close"));
        view.text("message", args.message() != null ? args.message() : BaniraComponent.get().literal(""));
        view.text("confirm", args.confirmText() != null ? args.confirmText() : BaniraComponent.get().transClientAuto("confirm"));
        view.text("cancel", args.cancelText() != null ? args.cancelText() : BaniraComponent.get().transClientAuto("cancel"));
        bindings.own(view.onAction("confirm", () -> { if (!resolved) { confirm(); view.requestClose(); } }));
        bindings.own(view.onAction("cancel", () -> { if (!resolved) { cancel(); view.requestClose(); } }));
        bindings.own(view.onAction("close", () -> { if (!resolved) { cancel(); view.requestClose(); } }));
    }
    @Override public void detach(UiDetachReason reason) { bindings.close(); }
    @Override public void close(UiCloseReason reason) {
        try { detach(UiDetachReason.SWITCH); }
        finally { if (reason == UiCloseReason.DISCONNECT) resolved = true; else cancel(); }
    }
}
