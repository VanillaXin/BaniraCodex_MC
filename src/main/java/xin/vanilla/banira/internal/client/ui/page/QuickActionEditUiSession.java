package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.internal.client.ui.UiRowPresentation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class QuickActionEditUiSession extends NavigationUiSession {
    private final Set<String> toolIds = new HashSet<>();
    private List<DropdownOption> icons;
    private Supplier<String> currentIcon;
    private Consumer<String> selectedIcon;
    private final Component heading;
    private Supplier<Component> context;
    private UiView currentView;
    private String query = "";

    public QuickActionEditUiSession(Component title, Supplier<List<Action>> rows, List<Action> tools) {
        super(title, rows, tools);
        heading = title;
        for (Action tool : tools) toolIds.add(tool.id());
    }

    public void context(Supplier<Component> value) {
        context = Objects.requireNonNull(value, "context");
    }

    public String query() {
        return query;
    }

    public void query(String text) {
        UiView owner = currentView;
        if (owner == null || !boundTo(owner)) return;
        String next = text == null ? "" : text;
        if (query.equals(next)) return;
        query = next;
        if (owner.state() != null) {
            owner.state().rowPosition(0);
            owner.state().scrollOffset(0);
            owner.state().scrollAnchor("");
        }
        refreshRows();
        if (!owner.nativeView()) updateEntries(owner);
    }

    @Override
    protected List<Action> visibleActions() {
        lombok.var filter = xin.vanilla.banira.client.gui.search.ConfigSearchQuery.of(query);
        List<Action> visible = new ArrayList<>();
        for (Action action : actions())
            if (filter.matches(action.id(), action.label().toVanilla().getString())) visible.add(action);
        return visible;
    }

    @Override
    protected UiRowProvider presentRows(UiRowProvider provider, UiView owner) {
        if (owner.nativeView()) return provider;
        List<Action> snapshot = visibleActions();
        return new UiRowPresentation() {
            public int rowHeight() {
                return 24;
            }

            public String rowClass(int index) {
                return "config-navigation";
            }

            public boolean textTools() {
                return true;
            }

            public int size() {
                return provider.size();
            }

            public String key(int index) {
                return provider.key(index);
            }

            public void bind(UiView row, int index) {
                if (!boundTo(owner)) return;
                provider.bind(row, index);
                row.tooltip("open", snapshot.get(index).label());
            }
        };
    }

    public void icons(List<DropdownOption> options, Supplier<String> current, Consumer<String> selected) {
        icons = new ArrayList<>(options);
        currentIcon = Objects.requireNonNull(current, "current");
        selectedIcon = Objects.requireNonNull(selected, "selected");
    }

    @Override
    public void bind(UiView view) {
        super.bind(view);
        if (!boundTo(view)) return;
        currentView = view;
        for (String id : Arrays.asList("details", "icon", "add_command", "add_screen", "add_menu", "save", "cancel", "add")) {
            view.enabled(id, toolIds.contains(id));
        }
        if (icons != null) {
            view.enabled("icon", true);
            view.control("icon", new UiControlSpec(UiControlSpec.Kind.SELECT, 128, "", false, icons, DropdownInputMode.SELECTION_ONLY));
            view.value("icon", UiValue.text(currentIcon.get()));
            view.tooltip("icon", BaniraComponent.get().transClientAuto("custom_quick_action_select_icon"));
            ownBinding(view.onValue("icon", value -> {
                if (boundTo(view) && icons.stream().anyMatch(option -> option.value().equals(value.asText())))
                    selectedIcon.accept(value.asText());
            }));
        }
        if (!view.nativeView()) {
            view.gameElement("brand", UiModuleHeaderIcons.resolve("banira_codex", xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().allEntriesInOrder()));
            view.text("module_name", BaniraComponent.get().transClientAuto("mod_name"));
            view.text("origin", BaniraComponent.get().transClientAuto("config_editor_client_scope"));
            view.text("category_title", heading);
            updateEntries(view);
            view.text("status", BaniraComponent.get().transClientAuto("config_editor_client_scope"));
            view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, 32767, "", false));
            view.draft("search", query);
            view.text("search_hint", BaniraComponent.get().transClientAuto("custom_quick_action_search_hint"));
            view.tooltip("search", BaniraComponent.get().transClientAuto("custom_quick_action_search_hint"));
            ownBinding(view.onDraft("search", value -> {
                if (boundTo(view)) query(value);
            }));
            view.text("clear_search", BaniraComponent.get().literal("\u00d7"));
            view.tooltip("clear_search", BaniraComponent.get().transClientAuto("config_editor_clear_search"));
            ownBinding(view.onAction("clear_search", () -> {
                if (boundTo(view)) {
                    query("");
                    view.draft("search", "");
                }
            }));
            view.text("footer_close", BaniraComponent.get().transClientAuto("close"));
            ownBinding(view.onAction("footer_close", () -> {
                if (boundTo(view)) view.requestClose();
            }));
        }
    }

    private void updateEntries(UiView view) {
        Component breadcrumb = context == null ? heading : context.get();
        view.text("breadcrumb", breadcrumb == null || breadcrumb.isEmpty() ? heading : breadcrumb);
        view.tooltip("breadcrumb", breadcrumb == null || breadcrumb.isEmpty() ? heading : breadcrumb);
        view.text("entry_count", BaniraComponent.get().literal(visibleActions().size() + " / " + actions().size()));
        view.text("empty", BaniraComponent.get().transClientAuto(toolIds.contains("save") ? "custom_quick_action_step_empty" : "custom_quick_action_empty"));
        view.enabled("empty", visibleActions().isEmpty());
    }

    @Override
    public void detach(UiDetachReason reason) {
        currentView = null;
        super.detach(reason);
    }
}
