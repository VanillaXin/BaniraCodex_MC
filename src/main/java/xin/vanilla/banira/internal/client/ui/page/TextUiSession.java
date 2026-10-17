package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.ReadOnlyTextScreen;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;

import java.util.Objects;

public final class TextUiSession implements UiPageSession {
    private final ReadOnlyTextScreen.Args args;
    private UiOwnedResources bindings = new UiOwnedResources();

    public TextUiSession(ReadOnlyTextScreen.Args args) { this.args = Objects.requireNonNull(args, "args"); }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD); bindings = new UiOwnedResources();
        view.text("title", args.title() != null ? args.title() : BaniraComponent.get().literal(""));
        Component text = BaniraComponent.get().literal("");
        if (args.paragraphs() != null) for (Component paragraph : args.paragraphs()) {
            if (paragraph == null) continue;
            if (!text.isEmpty()) text.append(BaniraComponent.get().literal("\n\n"));
            text.append(paragraph.clone());
        }
        view.text("text", text);
        view.text("close", args.closeText() != null ? args.closeText() : BaniraComponent.get().transClientAuto("close"));
        bindings.own(view.onAction("close", view::requestClose));
    }
    @Override public void detach(UiDetachReason reason) { bindings.close(); }
    @Override public void close(UiCloseReason reason) { detach(UiDetachReason.SWITCH); }
}
