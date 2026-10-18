package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.ConfigEditorInputLimits;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;

public final class PlayerConfigUiSession implements UiPageSession {
    private final FormUiSession fields;
    private UiOwnedResources bindings = new UiOwnedResources();
    private UiPageSpec page;
    private UiView view;
    private String query = "";
    private boolean closed;

    public PlayerConfigUiSession(InputFormScreen.Args args) { fields = new FormUiSession(args); }
    public FormUiSession fields() { return fields; }
    public String query() { return query; }
    public void query(String value) { query = value == null ? "" : value; applyFilter(); }
    public UiPageSpec spec(BaniraScreen screen, String surface) {
        if (page == null) page = new UiPageSpec(surface, "banira_codex/ui/player-config.html", this, previous -> screen, screen::getEffectiveTheme);
        return page;
    }
    private void applyFilter() {
        ConfigSearchQuery search = ConfigSearchQuery.of(query);
        fields.filter(widget -> search.matches(widget.name(),
                widget.title() == null ? "" : widget.title().toComponent().getString(Translator.getClientLanguage()),
                widget.tooltip() == null ? "" : widget.tooltip().toComponent().getString(Translator.getClientLanguage())));
    }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        if (closed) return;
        this.view = view;
        bindings = new UiOwnedResources();
        applyFilter();
        fields.bind(view);
        view.text("submit", BaniraComponent.get().transClientAuto("config_editor_save"));
        view.text("cancel", BaniraComponent.get().transClientAuto("config_editor_close"));
        view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, ConfigEditorInputLimits.STRING_VALUE, "", false));
        view.draft("search", query);
        bindings.own(view.onDraft("search", value -> { if (this.view == view) query(value); }));
    }
    @Override public void tick() { fields.tick(); }
    @Override public void detach(UiDetachReason reason) {
        view = null;
        try { bindings.close(); }
        finally { fields.detach(reason); }
    }
    @Override public void close(UiCloseReason reason) {
        if (closed) return;
        closed = true;
        try { detach(UiDetachReason.SWITCH); }
        finally { fields.close(reason); }
    }
}
