package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.ConfigEditorInputLimits;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;
import xin.vanilla.banira.internal.client.ui.UiRowPresentation;
import xin.vanilla.banira.common.data.Component;

public final class PlayerConfigUiSession implements UiPageSession {
    private final FormUiSession fields;
    private UiOwnedResources bindings = new UiOwnedResources();
    private UiPageSpec page;
    private UiView view;
    private String query = "";
    private boolean closed;
    private final java.util.function.Supplier<java.util.Map<String, String>> source;
    private final java.util.Map<String, String> baseline = new java.util.LinkedHashMap<>();
    private final java.util.Map<UiView, UiOwnedResources> categoryBindings = new java.util.LinkedHashMap<>();
    private PlayerConfigUiPresentation presentation;
    private boolean presentationLocked;
    private int visibleCount;
    private long renderedVersion = -1;
    private String renderedLanguage = "", renderedQuery = "", renderedRange = "", renderedError = "";

    public PlayerConfigUiSession(InputFormScreen.Args args) { this(args, null); }
    public PlayerConfigUiSession(InputFormScreen.Args args, java.util.function.Supplier<java.util.Map<String, String>> source) {
        this.source = source;
        fields = new FormUiSession(args, () -> source == null || source.get() != null, this::refresh);
        for (InputFormScreen.Widget widget : args.getWidgets()) baseline.put(widget.name(), widget.defaultValue());
    }
    public boolean refresh() {
        if (closed || source == null) return false;
        java.util.Map<String, String> snapshot = source.get();
        if (snapshot == null) return false;
        boolean changed = false;
        for (int index = 0; index < fields.args().getWidgets().size(); index++) {
            String name = fields.args().getWidgets().get(index).name(), value = snapshot.get(name);
            if (value == null) continue;
            String current = fields.draft(index);
            if (java.util.Objects.equals(current, baseline.get(name))) {
                fields.acceptNative(index, value);
                changed |= !java.util.Objects.equals(current, fields.draft(index));
            }
            baseline.put(name, value);
        }
        if (changed) applyFilter();
        return changed;
    }
    public FormUiSession fields() { return fields; }
    public PlayerConfigUiSession presentation(PlayerConfigUiPresentation value) {
        if (presentationLocked) throw new IllegalStateException("Player configuration presentation is already bound");
        presentation = java.util.Objects.requireNonNull(value, "presentation");
        return this;
    }
    public String query() { return query; }
    public void query(String value) {
        String next = value == null ? "" : value;
        if (presentation != null && !next.equals(query)) resetScroll();
        query = next;
        applyFilter();
        renderPresentation(false);
    }
    public UiPageSpec spec(BaniraScreen screen, String surface) {
        presentationLocked = true;
        if (page == null) page = new UiPageSpec(surface, presentation == null ? "banira_codex/ui/player-config.html"
                : "banira_codex/ui/player-config-editor.html", this, previous -> screen, screen::getEffectiveTheme);
        return page;
    }
    private void applyFilter() {
        ConfigSearchQuery search = ConfigSearchQuery.of(query);
        int[] count = {0};
        fields.filter(widget -> {
            boolean matches = search.matches(widget.name(),
                widget.title() == null ? "" : widget.title().toComponent().getString(Translator.getClientLanguage()),
                widget.tooltip() == null ? "" : widget.tooltip().toComponent().getString(Translator.getClientLanguage()));
            if (matches) count[0]++;
            return matches;
        });
        visibleCount = count[0];
    }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        if (closed) return;
        presentationLocked = true;
        refresh();
        this.view = view;
        bindings = new UiOwnedResources();
        applyFilter();
        if (presentation == null) fields.bind(view);
        else fields.bind(view, new FormUiSession.FieldPresentation() {
            public int rowHeight() { return 24; }
            public boolean checkbox(InputFormScreen.Widget widget) { return presentation.checkbox(widget.name()); }
            public String rowClass(int index) { return "player-config-field"; }
            public void bind(UiView row, int index, InputFormScreen.Widget widget) {
                row.text("key", BaniraComponent.get().literal(widget.name()));
                row.text("field_unit", presentation.unit());
            }
            public void update(UiView row, int index, InputFormScreen.Widget widget, String draft, String validation) {
                String language = Translator.getClientLanguage();
                String label = widget.tooltip() == null ? widget.title() == null ? widget.name()
                        : widget.title().toComponent().getString(language) : widget.tooltip().toComponent().getString(language);
                Component tooltip = BaniraComponent.get().literal(label + "\n" + widget.name() + "\n"
                        + presentation.rangeText().get().getString(language));
                row.text("tooltip", tooltip);
                row.tooltip("input", validation.isEmpty() ? tooltip : BaniraComponent.get().literal(validation));
            }
        });
        view.text("submit", BaniraComponent.get().transClientAuto("config_editor_save"));
        view.text("cancel", BaniraComponent.get().transClientAuto("config_editor_close"));
        view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, ConfigEditorInputLimits.STRING_VALUE, "", false));
        view.draft("search", query);
        bindings.own(view.onDraft("search", value -> { if (this.view == view) query(value); }));
        if (presentation == null) {
            view.text("clear_search", BaniraComponent.get().literal("\u00d7"));
            view.tooltip("clear_search", BaniraComponent.get().transClientAuto("config_editor_clear_search"));
            view.text("search_hint", BaniraComponent.get().transClientAuto("config_editor_search_hint"));
            bindings.own(view.onAction("clear_search", () -> { if (this.view == view) { query(""); view.draft("search", ""); } }));
        }
        if (presentation != null) {
            view.text("close", BaniraComponent.get().transClientAuto("config_editor_close"));
            bindings.own(view.onAction("close", () -> { if (this.view == view) view.requestClose(); }));
            view.text("clear_search", BaniraComponent.get().literal("\u00d7"));
            view.tooltip("clear_search", BaniraComponent.get().transClientAuto("config_editor_clear_search"));
            bindings.own(view.onAction("clear_search", () -> { if (this.view == view) { query(""); view.draft("search", ""); } }));
            renderPresentation(true);
        }
    }

    private void resetScroll() {
        if (view != null && view.state() != null) {
            view.state().selection("", 0, 0);
            view.state().rowPosition(0);
            view.state().scrollOffset(0);
        }
    }

    private void renderPresentation(boolean force) {
        if (presentation == null || view == null || closed) return;
        String language = Translator.getClientLanguage();
        Component range = presentation.rangeText().get();
        String rangeText = range.getString(language);
        boolean languageChanged = !language.equals(renderedLanguage);
        if (!force && fields.version() == renderedVersion && query.equals(renderedQuery)
                && !languageChanged && rangeText.equals(renderedRange) && fields.error().equals(renderedError)) return;
        if (!force && languageChanged) applyFilter();
        fields.updateStatus();
        view.text("module_name", presentation.moduleName());
        view.text("origin", BaniraComponent.get().transClientAuto("player_config_personal"));
        view.text("category_title", BaniraComponent.get().transClientAuto("config_editor_categories"));
        view.text("category_count", BaniraComponent.get().literal("1"));
        view.text("breadcrumb", presentation.sectionTitle());
        view.tooltip("breadcrumb", presentation.sectionTitle());
        view.text("entry_count", BaniraComponent.get().literal(visibleCount + " / " + fields.args().getWidgets().size()));
        view.text("range", range);
        view.text("search_hint", BaniraComponent.get().transClientAuto("config_editor_search_hint"));
        view.text("empty", BaniraComponent.get().transClientAuto("config_editor_empty"));
        view.enabled("empty", visibleCount == 0);
        view.enabled("error", !fields.error().isEmpty());
        int changed = 0;
        for (InputFormScreen.Widget widget : fields.args().getWidgets())
            if (!java.util.Objects.equals(fields.draft(widget.name()), baseline.get(widget.name()))) changed++;
        view.text("status", !fields.errors().isEmpty() ? BaniraComponent.get().transClientAuto("player_config_invalid_status", fields.errors().size())
                : changed == 0 ? BaniraComponent.get().transClientAuto("config_editor_clean_status")
                : BaniraComponent.get().transClientAuto("config_editor_pending_status", changed));
        if (force || languageChanged) {
            var brand = UiModuleHeaderIcons.resolve(presentation.namespace(), xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().allEntriesInOrder());
            view.gameElement("brand", brand); view.enabled("brand", brand != null);
            bindCategory();
        }
        renderedVersion = fields.version(); renderedQuery = query; renderedLanguage = language; renderedRange = rangeText; renderedError = fields.error();
    }

    private void bindCategory() {
        for (UiOwnedResources owned : categoryBindings.values()) owned.close();
        categoryBindings.clear();
        UiView owner = view;
        view.rows("categories", new UiRowPresentation() {
            public int rowHeight() { return 20; }
            public String scrollerId() { return "category_content"; }
            public boolean tracksPageScroll() { return false; }
            public boolean textTools() { return true; }
            public String rowClass(int index) { return "config-navigation config-selected"; }
            public int size() { return 1; }
            public String key(int index) { return "player:countdown"; }
            public void bind(UiView row, int index) {
                if (view != owner) return;
                UiOwnedResources old = categoryBindings.remove(row);
                if (old != null) old.close();
                UiOwnedResources owned = new UiOwnedResources(); categoryBindings.put(row, owned);
                row.text("open", presentation.sectionTitle()); row.tooltip("open", presentation.sectionTitle());
                row.text("count", BaniraComponent.get().literal(String.valueOf(fields.args().getWidgets().size())));
                owned.own(row.onAction("open", () -> {
                    if (view != owner) return;
                    resetScroll(); applyFilter(); renderPresentation(true);
                }));
            }
        }, 1);
    }

    @Override public void tick() { refresh(); fields.tick(); renderPresentation(false); }
    @Override public void detach(UiDetachReason reason) {
        view = null;
        try {
            bindings.close();
            for (UiOwnedResources owned : categoryBindings.values()) owned.close();
            categoryBindings.clear();
        }
        finally { fields.detach(reason); }
    }
    @Override public void close(UiCloseReason reason) {
        if (closed) return;
        closed = true;
        try { detach(UiDetachReason.SWITCH); }
        finally { fields.close(reason); }
    }
}
