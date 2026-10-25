package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;

import java.util.*;
import java.util.function.Supplier;

public class NavigationUiSession implements UiPageSession {
    public static final class Action {
        private final String id;
        private final Component label;
        private final Runnable open, delete;
        public Action(String id, Component label, Runnable open, Runnable delete) {
            this.id = Objects.requireNonNull(id, "id"); this.label = Objects.requireNonNull(label, "label");
            this.open = Objects.requireNonNull(open, "open"); this.delete = delete;
        }
        public String id() { return id; }
        public Component label() { return label; }
        public void open() { open.run(); }
    }

    private final Component title;
    private final Supplier<List<Action>> rows;
    private final List<Action> tools;
    private final Map<UiView, UiOwnedResources> rowBindings = new LinkedHashMap<>();
    private UiOwnedResources bindings = new UiOwnedResources();
    private UiView view;
    private UiPageSpec page;
    private boolean closed;
    private Runnable longClose;

    public NavigationUiSession(Component title, List<Action> actions) {
        this(title, () -> actions, Collections.emptyList());
    }

    protected NavigationUiSession(Component title, Supplier<List<Action>> rows, List<Action> tools) {
        this.title = Objects.requireNonNull(title, "title");
        this.rows = Objects.requireNonNull(rows, "rows");
        this.tools = Collections.unmodifiableList(new ArrayList<>(tools));
    }

    public List<Action> actions() { return Collections.unmodifiableList(new ArrayList<>(rows.get())); }
    protected List<Action> visibleActions() { return actions(); }
    protected UiRowProvider presentRows(UiRowProvider provider, UiView owner) {
        return page == null || page.template().equals("banira_codex/ui/navigation.html")
                ? new UiCommonRows(provider, 24, "config-navigation") : provider;
    }
    public void longClose(Runnable action) { longClose = action; }
    protected final boolean boundTo(UiView view) { return !closed && this.view == view; }
    protected final void ownBinding(AutoCloseable resource) { bindings.own(resource); }
    public final void refresh() { UiView current = view; if (!closed && current != null) bind(current); }
    public UiPageSpec spec(BaniraScreen screen, String surface, String template) {
        if (page == null) page = new UiPageSpec(surface, template, this, previous -> screen, screen::getEffectiveTheme);
        return page;
    }

    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        if (closed) return;
        this.view = Objects.requireNonNull(view, "view");
        bindings = new UiOwnedResources();
        view.text("title", title);
        if (page == null || page.template().equals("banira_codex/ui/navigation.html")) UiCommonChrome.bind(view, title, null);
        view.text("close", BaniraComponent.get().transClientAuto("close"));
        bindings.own(view.onAction("close", () -> { if (this.view == view) view.requestClose(); }));
        if (longClose != null) bindings.own(view.onLongAction("close", () -> { if (this.view == view) longClose.run(); }));
        for (Action action : tools) {
            view.text(action.id, action.label);
            view.enabled(action.id, true);
            bindings.own(view.onAction(action.id, () -> { if (this.view == view) action.open(); }));
        }
        refreshRows();
    }

    protected final void refreshRows() {
        UiView view = this.view;
        if (view == null || closed) return;
        for (UiOwnedResources owned : rowBindings.values()) owned.close();
        rowBindings.clear();
        List<Action> snapshot = visibleActions();
        UiRowProvider provider = new UiRowProvider() {
            public int size() { return snapshot.size(); }
            public String key(int index) { return snapshot.get(index).id; }
            public void bind(UiView row, int index) {
                if (NavigationUiSession.this.view != view) return;
                UiOwnedResources old = rowBindings.remove(row);
                if (old != null) old.close();
                if (rowBindings.size() >= 120) {
                    UiView first = rowBindings.keySet().iterator().next(); rowBindings.remove(first).close();
                }
                UiOwnedResources owned = new UiOwnedResources(); rowBindings.put(row, owned);
                Action action = snapshot.get(index);
                row.text("open", action.label);
                owned.own(row.onAction("open", () -> { if (NavigationUiSession.this.view == view) action.open(); }));
                if (action.delete != null) {
                    row.text("delete", BaniraComponent.get().literal("\u00d7"));
                    row.tooltip("delete", BaniraComponent.get().transClientAuto("delete"));
                    owned.own(row.onAction("delete", () -> { if (NavigationUiSession.this.view == view) action.delete.run(); }));
                }
            }
        };
        view.rows("fields", presentRows(provider, view), 120);
    }

    @Override public void detach(UiDetachReason reason) {
        view = null;
        try { bindings.close(); }
        finally {
            for (UiOwnedResources owned : rowBindings.values()) owned.close();
            rowBindings.clear();
        }
    }
    @Override public void close(UiCloseReason reason) { if (!closed) { closed = true; detach(UiDetachReason.SWITCH); } }
}
