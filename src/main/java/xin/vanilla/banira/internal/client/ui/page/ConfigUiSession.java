package xin.vanilla.banira.internal.client.ui.page;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.DropdownSelectWidget;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.common.network.packet.ConfigSyncToServer;
import xin.vanilla.banira.internal.client.*;
import xin.vanilla.banira.internal.client.ui.*;

import java.util.*;

public final class ConfigUiSession implements UiPageSession, UiCloseGuard {
    public interface Services {
        boolean connected();
        void sync(ConfigHolder holder, Map<String, String> values);
        void fetch(ConfigHolder holder);
        void notify(String key, long duration, Object... arguments);
        void execute(Runnable action);
    }

    private final ConfigHolder holder;
    private final ConfigEditorState state;
    private final Services services;
    private final Map<String, Slot> slots = new LinkedHashMap<>();
    private Runnable unsubscribe;
    private final ConfigEditorScreen.Args args;
    private ConfigEditorScreen nativeScreen;
    private UiPageSpec spec;
    private UiView view;
    private UiOwnedResources bindings = new UiOwnedResources();
    private final Map<UiView, UiOwnedResources> rowBindings = new LinkedHashMap<>();
    private final Map<UiView, ConfigEntryDescriptor> boundEntries = new LinkedHashMap<>();
    private final Map<UiView, Row> boundListRows = new LinkedHashMap<>();
    private final Map<String, Boolean> expanded = new LinkedHashMap<>();
    private List<Row> visibleRows = Collections.emptyList();
    private String queryText = "";
    private boolean closed, saving;

    public ConfigUiSession(ConfigHolder holder, ConfigEditorScreen.Args args) {
        this(holder, args, new Services() {
            public boolean connected() { return ConfigEditorSyncService.hasServerConnection(); }
            public void sync(ConfigHolder target, Map<String, String> values) { ConfigEditorSyncService.sendSync(target, values); }
            public void fetch(ConfigHolder target) { ConfigEditorSyncService.requestSnapshot(target); }
            public void notify(String key, long duration, Object... arguments) { ConfigEditorNotifier.show(key, duration, arguments); }
            public void execute(Runnable action) { Minecraft.getInstance().execute(action); }
        });
    }

    public ConfigUiSession(ConfigHolder holder, ConfigEditorScreen.Args args, Services services) {
        this.holder = Objects.requireNonNull(holder, "holder");
        this.args = args != null ? args : new ConfigEditorScreen.Args();
        this.services = Objects.requireNonNull(services, "services");
        state = new ConfigEditorState(holder);
        for (ConfigEntryDescriptor descriptor : holder.getDescriptors()) {
            Object value = holder.get(descriptor.getPath());
            Slot slot = new Slot(descriptor, value != null ? value : descriptor.getDefaultValue());
            slots.put(descriptor.getPath(), slot);
            state.registerEntry(descriptor.getPath(), slot);
        }
        subscribe();
    }

    public void attachNative(ConfigEditorScreen screen) { nativeScreen = screen; }
    public void activateNative() { if (!closed) subscribe(); }
    public void removedNative(ConfigEditorScreen screen) {
        if (view == null || view instanceof NativeUiView && ((NativeUiView) view).screen() == screen) unsubscribe();
    }
    private void subscribe() {
        if (unsubscribe == null) unsubscribe = holder.onReloaded(changed -> services.execute(() -> refresh(holder.getConfigName())));
    }
    private void unsubscribe() { if (unsubscribe != null) { unsubscribe.run(); unsubscribe = null; } }
    public UiPageSpec spec() {
        if (spec == null) spec = new UiPageSpec("banira_codex:config", "banira_codex/ui/config.html", this,
                previous -> nativeScreen != null ? nativeScreen : new ConfigEditorScreen(holder, args, this),
                () -> nativeScreen != null ? nativeScreen.getEffectiveTheme() : args.theme() != null ? args.theme()
                        : args.parentScreen() instanceof xin.vanilla.banira.client.gui.BaniraScreen
                        ? ((xin.vanilla.banira.client.gui.BaniraScreen) args.parentScreen()).getEffectiveTheme()
                        : xin.vanilla.banira.client.util.ClientThemeManager.getEffectiveTheme());
        return spec;
    }

    public ConfigEditorState state() { return state; }
    public String query() { return queryText; }
    public void query(String query) { queryText = query == null ? "" : query; }
    public Map<String, Boolean> categoryExpansion() { return Collections.unmodifiableMap(new LinkedHashMap<>(expanded)); }
    public void categoryExpansion(String path, boolean open) { if (!closed) expanded.put(path, open); }
    public Object value(String path) { return entry(path).getValue(); }
    public String draft(String path) { return entry(path).getDraft(); }

    public void draft(String path, String text) {
        if (closed) return;
        entry(path).setDraft(Objects.requireNonNull(text, "text"));
        state.markModified(path, entry(path).getValue());
        updateStatus();
    }

    public void reset(String path) {
        if (closed) return;
        ConfigEntryDescriptor descriptor = holder.getDescriptor(path);
        if (descriptor == null) throw new IllegalArgumentException("Unknown config path: " + path);
        entry(path).setValue(descriptor.getDefaultValue());
        state.markModified(path, entry(path).getValue());
        updateVisibleValues();
    }

    private ConfigEditorEntryWidget entry(String path) {
        ConfigEditorEntryWidget entry = state.entryWidgets().get(path);
        if (entry == null) throw new IllegalArgumentException("Unknown config path: " + path);
        return entry;
    }

    public boolean save() {
        if (closed || saving) return false;
        if (state.isRemoteSnapshot()) return sync(false);
        if (state.hasInvalidEntryWidgets()) { notice("config_editor_validation_failed", 3000); return false; }
        saving = true;
        try {
            state.collectModifiedFromWidgets();
            state.applyModifiedToHolder();
            holder.save();
            state.markClean();
            notice("config_editor_save_success", 2000);
            return true;
        } catch (Exception failure) {
            notice("config_editor_save_failed", 4000, detail(failure));
            return false;
        } finally { saving = false; }
    }

    public boolean sync(boolean all) {
        if (closed || saving || !holder.canSyncToServer()) return false;
        if (!services.connected()) { notice("config_editor_sync_not_connected", 3500); return false; }
        if (state.hasInvalidEntryWidgets()) { notice("config_editor_validation_failed", 3000); return false; }
        Map<String, Object> values = all ? state.collectAllEntryValuesForSync() : state.collectTouchedPathsForSync();
        if (values.isEmpty()) { notice("config_editor_sync_nothing", 2500); return false; }
        saving = true;
        try {
            services.sync(holder, ConfigEditorSyncService.encodePayload(values));
            return true;
        } catch (Exception failure) {
            notice(all ? "config_editor_sync_full_failed" : "config_editor_sync_failed", 4000, detail(failure));
            return false;
        } finally { saving = false; }
    }

    public void fetch() {
        if (closed || !holder.canSyncToServer()) return;
        if (!services.connected()) { notice("config_editor_fetch_not_connected", 3500); return; }
        try { services.fetch(holder); }
        catch (Exception failure) { notice("config_editor_fetch_send_failed", 4000, detail(failure)); }
    }

    public void acceptRemoteSnapshot(String configName, Map<String, Object> snapshot) {
        if (!closed) { state.acceptRemoteSnapshot(configName, snapshot); updateVisibleValues(); }
    }

    public void refresh(String configName) {
        if (!closed) { state.refreshEntriesFromHolder(configName); updateVisibleValues(); }
    }

    @Override public boolean canClose(UiCloseReason reason) {
        int pending = state.pendingChangeCount();
        if (reason != UiCloseReason.USER_CLOSE || pending == 0 || discardClose) return true;
        notice("config_editor_unsaved_changes", 4500, pending);
        return false;
    }

    public List<String> search(String text) {
        ConfigSearchQuery query = ConfigSearchQuery.of(text == null ? "" : text);
        Set<String> categories = new LinkedHashSet<>();
        for (ConfigHolder.CategoryTreeNode root : holder.getCategoryTree()) matchingCategories(root, query, categories);
        List<String> matches = new ArrayList<>();
        for (ConfigEntryDescriptor descriptor : holder.getDescriptors()) {
            if (query.isEmpty() || query.matches(holder.getConfigName()) || query.matches(descriptor.getPath(), descriptor.getDisplayName(),
                    tooltip(descriptor).getString(Translator.getClientLanguage()))
                    || categories.stream().anyMatch(path -> descriptor.getPath().startsWith(path + "."))) {
                matches.add(descriptor.getPath());
            }
        }
        return matches;
    }

    private Component tooltip(ConfigEntryDescriptor descriptor) {
        return ConfigEntryTooltipTexts.guiTooltipComponent(descriptor, holder.getModId());
    }

    private void matchingCategories(ConfigHolder.CategoryTreeNode node, ConfigSearchQuery query, Set<String> paths) {
        if (!query.isEmpty() && query.matches(node.getCategoryPath(), categoryTitle(node).getString(Translator.getClientLanguage()))) {
            paths.add(node.getCategoryPath());
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) matchingCategories(child, query, paths);
    }

    private Component categoryTitle(ConfigHolder.CategoryTreeNode node) {
        return ConfigCategoryTitleTexts.categoryTitleComponent(holder.getCategoryTitleSpec(node.getCategoryPath()), holder.getModId(), node.getDisplayName());
    }

    private void notice(String key, long duration, Object... arguments) { services.notify(key, duration, arguments); }
    private static String detail(Exception failure) { return failure.getMessage() != null ? failure.getMessage() : ""; }

    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        if (closed) return;
        this.view = Objects.requireNonNull(view, "view");
        subscribe();
        bindings = new UiOwnedResources();
        if (view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof ConfigEditorScreen) return;
        state.clearEntries();
        slots.forEach(state::registerEntry);
        view.text("title", BaniraComponent.get().transClientAuto("config_editor_title"));
        view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, ConfigEditorInputLimits.STRING_VALUE, "", false));
        view.draft("search", queryText);
        bindings.own(view.onDraft("search", text -> {
            if (this.view == view) { queryText = text; rebuildRows(); }
        }));
        view.text("save", BaniraComponent.get().transClientAuto("config_editor_save"));
        view.tooltip("save", BaniraComponent.get().transClientAuto(holder.canSyncToServer() ? "config_editor_save_tooltip_network" : "config_editor_save_tooltip"));
        view.text("sync", BaniraComponent.get().transClientAuto("config_editor_sync"));
        view.tooltip("sync", BaniraComponent.get().transClientAuto("config_editor_sync_tooltip"));
        view.text("close", BaniraComponent.get().transClientAuto("config_editor_close"));
        view.enabled("sync", holder.canSyncToServer());
        bindings.own(view.onAction("save", () -> { if (this.view == view) save(); }));
        bindings.own(view.onAction("sync", () -> { if (this.view == view) sync(false); }));
        if (holder.canSyncToServer()) {
            bindings.own(view.onLongAction("save", () -> { if (this.view == view) fetch(); }));
            bindings.own(view.onLongAction("sync", () -> { if (this.view == view) sync(true); }));
        }
        bindings.own(view.onAction("close", () -> {
            if (this.view == view) { discardClose = true; view.requestClose(); }
        }));
        rebuildRows();
    }

    private boolean discardClose;

    private void rebuildRows() {
        if (view == null) return;
        for (UiOwnedResources owned : rowBindings.values()) owned.close();
        rowBindings.clear(); boundEntries.clear(); boundListRows.clear();
        List<Row> rows = new ArrayList<>();
        Set<String> matches = new LinkedHashSet<>(search(queryText));
        for (ConfigHolder.CategoryTreeNode root : holder.getCategoryTree()) appendRows(root, matches, rows);
        visibleRows = rows;
        UiView owner = view;
        view.rows("fields", new UiRowProvider() {
            public int size() { return visibleRows.size(); }
            public String key(int index) { return visibleRows.get(index).key(); }
            public void bind(UiView row, int index) {
                if (ConfigUiSession.this.view != owner || index >= visibleRows.size()) return;
                UiOwnedResources old = rowBindings.remove(row);
                if (old != null) old.close();
                boundEntries.remove(row);
                boundListRows.remove(row);
                if (rowBindings.size() >= 120) {
                    UiView first = rowBindings.keySet().iterator().next();
                    rowBindings.remove(first).close(); boundEntries.remove(first); boundListRows.remove(first);
                }
                UiOwnedResources owned = new UiOwnedResources(); rowBindings.put(row, owned);
                Row data = visibleRows.get(index);
                if (data.descriptor == null) {
                    row.text("open", BaniraComponent.get().literal((expanded.getOrDefault(data.path, false) ? "- " : "+ ")
                            + data.title.getString(Translator.getClientLanguage())));
                    row.tooltip("open", data.title);
                    owned.own(row.onAction("open", () -> {
                        if (ConfigUiSession.this.view != owner) return;
                        expanded.put(data.path, !expanded.getOrDefault(data.path, false)); rebuildRows();
                    }));
                } else {
                    ConfigEntryDescriptor descriptor = data.descriptor;
                    if (isList(descriptor)) {
                        bindListRow(row, data, owned, owner);
                        return;
                    }
                    boundEntries.put(row, descriptor);
                    row.text("label", BaniraComponent.get().literal(descriptor.getDisplayName()));
                    row.text("tooltip", tooltip(descriptor));
                    row.text("reset", BaniraComponent.get().literal("\u21ba"));
                    row.tooltip("reset", BaniraComponent.get().transClientAuto("config_editor_reset_tooltip"));
                    owned.own(row.onAction("reset", () -> { if (ConfigUiSession.this.view == owner) reset(descriptor.getPath()); }));
                    configureRow(row, descriptor);
                    if (descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.BOOLEAN
                            || descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM) {
                        owned.own(row.onValue("input", value -> {
                            if (ConfigUiSession.this.view == owner) draft(descriptor.getPath(),
                                    value.kind() == UiValue.Kind.BOOLEAN ? String.valueOf(value.asBoolean()) : value.asText());
                        }));
                    } else owned.own(row.onDraft("input", text -> {
                        if (ConfigUiSession.this.view == owner) draft(descriptor.getPath(), text);
                    }));
                }
            }
        }, 120);
        updateStatus();
    }

    private void appendRows(ConfigHolder.CategoryTreeNode node, Set<String> matches, List<Row> rows) {
        String path = node.getCategoryPath();
        boolean present = path.isEmpty() || matches.stream().anyMatch(key -> key.startsWith(path + "."));
        if (!present) return;
        if (!path.isEmpty()) rows.add(new Row(path, null, categoryTitle(node)));
        if (!path.isEmpty() && queryText.isBlank() && !expanded.getOrDefault(path, false)) return;
        for (ConfigEntryDescriptor descriptor : node.getEntries()) if (matches.contains(descriptor.getPath())) {
            rows.add(new Row(descriptor.getPath(), descriptor, null));
            if (isList(descriptor)) {
                ConfigListDraft draft = listDraft(descriptor.getPath());
                if (draft.expanded) {
                    int size = ((List<?>) value(descriptor.getPath())).size();
                    for (int index = 0; index <= size; index++) rows.add(new Row(descriptor.getPath(), descriptor, null, index));
                }
            }
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) appendRows(child, matches, rows);
    }

    private void configureRow(UiView row, ConfigEntryDescriptor descriptor) {
        String path = descriptor.getPath();
        if (descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.BOOLEAN) {
            row.value("input", UiValue.bool(Boolean.TRUE.equals(value(path))));
        } else if (descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM) {
            row.control("input", new UiControlSpec(UiControlSpec.Kind.SELECT, ConfigEditorInputLimits.STRING_VALUE, "", false,
                    DropdownSelectWidget.enumOptions(descriptor.getEnumClass()), DropdownInputMode.SELECTION_ONLY));
            row.value("input", UiValue.text(draft(path)));
        } else {
            UiControlSpec.Kind kind = descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.INTEGER
                    || descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.LONG
                    || descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.DOUBLE ? UiControlSpec.Kind.DECIMAL : UiControlSpec.Kind.TEXT;
            row.control("input", new UiControlSpec(kind, ConfigEditorInputLimits.STRING_VALUE, "", false));
            row.draft("input", draft(path));
        }
    }

    private void updateVisibleValues() {
        if (view != null && !(view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof ConfigEditorScreen)) {
            rebuildRows(); return;
        }
        for (Map.Entry<UiView, ConfigEntryDescriptor> row : boundEntries.entrySet()) configureRow(row.getKey(), row.getValue());
        updateStatus();
    }

    private void updateStatus() {
        if (view == null) return;
        view.enabled("save", !state.hasInvalidEntryWidgets());
        for (Map.Entry<UiView, ConfigEntryDescriptor> row : boundEntries.entrySet()) row.getKey().text("validation",
                entry(row.getValue().getPath()).isValid() ? BaniraComponent.get().literal("")
                        : BaniraComponent.get().transClientAuto("config_editor_validation_failed"));
        for (Map.Entry<UiView, Row> item : boundListRows.entrySet()) {
            Row data = item.getValue();
            if (data.listIndex < 0) continue;
            ConfigListDraft draft = listDraft(data.path);
            int active = draft.editingIndex >= 0 ? draft.editingIndex
                    : draft.adding && (!draft.addText.isEmpty() || !draft.selected.isEmpty()) ? ((List<?>) value(data.path)).size() : -1;
            boolean enabled = active < 0 || active == data.listIndex;
            item.getKey().enabled("input", enabled); item.getKey().enabled("apply", enabled); item.getKey().enabled("delete", enabled);
        }
    }

    @Override public void detach(UiDetachReason reason) {
        view = null;
        bindings.close();
        for (UiOwnedResources owned : rowBindings.values()) owned.close();
        rowBindings.clear(); boundEntries.clear(); boundListRows.clear();
    }
    @Override public void close(UiCloseReason reason) {
        if (closed) return;
        closed = true;
        try { detach(UiDetachReason.DISCONNECT); }
        finally { unsubscribe(); }
    }

    private static final class Row {
        final String path;
        final ConfigEntryDescriptor descriptor;
        final Component title;
        final int listIndex;
        Row(String path, ConfigEntryDescriptor descriptor, Component title) { this(path, descriptor, title, -1); }
        Row(String path, ConfigEntryDescriptor descriptor, Component title, int listIndex) {
            this.path = path; this.descriptor = descriptor; this.title = title; this.listIndex = listIndex;
        }
        String key() { return (descriptor == null ? "category:" : "config:") + path + (listIndex >= 0 ? "/item:" + listIndex : ""); }
    }

    private static boolean isList(ConfigEntryDescriptor descriptor) { return descriptor.isListType(); }
    private ConfigListDraft listDraft(String path) { return ConfigListDraft.decode(draft(path)); }
    private void listDraft(String path, ConfigListDraft draft) { draft(path, draft.encode()); }

    private void bindListRow(UiView row, Row data, UiOwnedResources owned, UiView owner) {
        String path = data.path;
        ConfigEntryDescriptor descriptor = data.descriptor;
        ConfigListDraft draft = listDraft(path);
        List<?> values = (List<?>) value(path);
        boundListRows.put(row, data);
        if (data.listIndex < 0) {
            row.text("open", BaniraComponent.get().literal((draft.expanded ? "- " : "+ ") + descriptor.getDisplayName() + " (" + values.size() + ")"));
            row.tooltip("open", tooltip(descriptor));
            owned.own(row.onAction("open", () -> {
                if (view != owner) return;
                ConfigListDraft next = listDraft(path); next.expanded = !next.expanded;
                listDraft(path, next); rebuildRows();
            }));
            row.text("reset", BaniraComponent.get().literal("\u21ba"));
            row.tooltip("reset", BaniraComponent.get().transClientAuto("config_editor_reset_tooltip"));
            owned.own(row.onAction("reset", () -> { if (view == owner) reset(path); }));
            row.text("delete", BaniraComponent.get().literal(draft.undo == null ? "\u00d7" : "\u21b6"));
            row.tooltip("delete", BaniraComponent.get().transClientAuto(draft.undo == null ? "tag_list_clear_tooltip" : "tag_list_undo_tooltip"));
            owned.own(row.onAction("delete", () -> {
                if (view != owner) return;
                ConfigListDraft next = listDraft(path);
                List<?> replacement = next.undo == null ? Collections.emptyList() : ConfigListSpecHelper.parseNetworkList(next.undo, descriptor);
                String undo = next.undo == null ? ConfigListSpecHelper.encodeNetworkList((List<?>) value(path)) : null;
                boolean expanded = next.expanded;
                entry(path).setValue(replacement); next = new ConfigListDraft(); next.expanded = expanded; next.undo = undo;
                listDraft(path, next); rebuildRows();
            }));
            return;
        }
        int index = data.listIndex;
        boolean add = index == values.size();
        row.text("label", add ? BaniraComponent.get().transClientAuto("tag_list_add_tooltip") : BaniraComponent.get().literal(String.valueOf(index + 1)));
        row.text("tooltip", tooltip(descriptor));
        String text = add ? draft.addText : draft.editingIndex == index ? draft.editText : scalarText(values.get(index));
        configureListInput(row, descriptor, text == null ? "" : text, add ? draft.selected : null);
        row.text("apply", BaniraComponent.get().literal(add ? "+" : "\u2713"));
        row.tooltip("apply", BaniraComponent.get().transClientAuto(add ? "tag_list_confirm_add" : "config_editor_save"));
        Runnable apply = () -> { if (view == owner) commitListItem(path, index); };
        owned.own(row.onAction("apply", apply));
        row.text("delete", BaniraComponent.get().literal("\u00d7"));
        row.tooltip("delete", BaniraComponent.get().transClientAuto("config_editor_close"));
        owned.own(row.onAction("delete", () -> {
            if (view != owner) return;
            List<Object> next = new ArrayList<>((List<?>) value(path));
            if (!add) next.remove(index);
            ConfigListDraft retained = listDraft(path);
            retained.editingIndex = -1; retained.editText = null; retained.addText = ""; retained.selected.clear(); retained.raw = null;
            entry(path).setValue(next); listDraft(path, retained); rebuildRows();
        }));
        boolean selected = descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.BOOLEAN_LIST
                || descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM_LIST || descriptor.isKeyChords();
        if (selected) owned.own(row.onValue("input", value -> {
            if (view != owner) return;
            if (value.kind() == UiValue.Kind.STRING_LIST) {
                ConfigListDraft next = listDraft(path); next.adding = true; next.addText = "";
                next.selected = new ArrayList<>(value.asStrings()); listDraft(path, next);
            } else editListItem(path, index, value.kind() == UiValue.Kind.KEY_CHORDS ? String.join("+", value.asKeyChords()) : value.asText());
        }));
        else owned.own(row.onDraft("input", value -> { if (view == owner) editListItem(path, index, value); }));
    }

    private void configureListInput(UiView row, ConfigEntryDescriptor descriptor, String text, List<String> selected) {
        UiControlSpec.Kind kind;
        List<xin.vanilla.banira.client.gui.widget.DropdownOption> options = Collections.emptyList();
        switch (descriptor.getValueType()) {
            case BOOLEAN_LIST:
                kind = UiControlSpec.Kind.SELECT;
                options = Arrays.asList(new xin.vanilla.banira.client.gui.widget.DropdownOption("true"), new xin.vanilla.banira.client.gui.widget.DropdownOption("false")); break;
            case ENUM_LIST: kind = UiControlSpec.Kind.SELECT; options = DropdownSelectWidget.enumOptions(descriptor.getEnumClass()); break;
            case INTEGER_LIST: case LONG_LIST: case DOUBLE_LIST: kind = UiControlSpec.Kind.DECIMAL; break;
            default: kind = descriptor.isKeyChords() ? UiControlSpec.Kind.KEY_CHORD : UiControlSpec.Kind.TEXT;
        }
        boolean multiple = kind == UiControlSpec.Kind.SELECT && selected != null;
        row.control("input", new UiControlSpec(kind, ConfigEditorInputLimits.LIST_ITEM, "", multiple, options, DropdownInputMode.SELECTION_ONLY));
        if (multiple) row.value("input", UiValue.strings(selected.isEmpty() && !text.isEmpty() ? Collections.singletonList(text) : selected));
        else if (kind == UiControlSpec.Kind.SELECT) row.value("input", UiValue.text(text));
        else if (kind == UiControlSpec.Kind.KEY_CHORD) row.value("input", UiValue.keyChords(text.isEmpty() ? Collections.emptyList() : Arrays.asList(text.split("\\+"))));
        else row.draft("input", text);
    }

    private void editListItem(String path, int index, String text) {
        ConfigListDraft draft = listDraft(path);
        int size = ((List<?>) value(path)).size();
        if (index == size) { draft.adding = true; draft.addText = text; draft.selected.clear(); }
        else { draft.editingIndex = index; draft.editText = text; }
        listDraft(path, draft);
    }

    private void commitListItem(String path, int index) {
        ConfigListDraft draft = listDraft(path);
        ConfigEntryDescriptor descriptor = holder.getDescriptor(path);
        List<Object> values = new ArrayList<>((List<?>) value(path));
        boolean add = index == values.size();
        String text = add ? draft.addText : draft.editingIndex == index ? draft.editText : scalarText(values.get(index));
        List<String> pending = add && !draft.selected.isEmpty() ? draft.selected : Collections.singletonList(text);
        if (add && pending.size() == 1 && (descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.BOOLEAN_LIST
                || descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.ENUM_LIST
                || descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.STRING_LIST && !descriptor.isKeyChords())
                && (pending.get(0) == null || pending.get(0).isBlank())) return;
        for (String raw : pending) {
            if (descriptor.getValueType() == ConfigEntryDescriptor.ConfigValueType.STRING_LIST && raw != null) raw = raw.trim();
            Object parsed = ConfigListSpecHelper.coerceListElement(raw, descriptor.getValueType(), descriptor.getEnumClass(),
                    descriptor.getMinValue(), descriptor.getMaxValue(), descriptor.getDecimalPlaces());
            if (parsed == null) { notice("config_editor_validation_failed", 3000); return; }
            if (add) values.add(parsed); else values.set(index, parsed);
        }
        try { ConfigListSpecHelper.parseNetworkList(ConfigListSpecHelper.encodeNetworkList(values), descriptor); }
        catch (IllegalArgumentException invalid) { notice("config_editor_validation_failed", 3000); return; }
        if (!holder.validate(path, values)) { notice("config_editor_validation_failed", 3000); return; }
        if (add) { draft.addText = ""; draft.selected.clear(); }
        else { draft.editingIndex = -1; draft.editText = null; }
        draft.raw = null; draft.undo = null;
        entry(path).setValue(values); listDraft(path, draft); rebuildRows();
    }

    private static String scalarText(Object value) { return value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value); }

    private final class Slot implements ConfigEditorEntryWidget {
        final ConfigEntryDescriptor descriptor;
        Object value;
        String draft;
        boolean valid;
        ConfigListDraft list;
        Slot(ConfigEntryDescriptor descriptor, Object value) { this.descriptor = descriptor; setValue(value); }
        public BaseWidget getWidget() { return null; }
        public Object getValue() { return value; }
        public String getDraft() { return list != null ? list.encode() : draft; }
        public boolean isValid() { return valid && (list == null || !list.pending()); }
        public void setValue(Object value) {
            this.value = value instanceof List ? new ArrayList<>((List<?>) value) : value;
            draft = value instanceof List ? ConfigListSpecHelper.encodeNetworkList((List<?>) value)
                    : value instanceof Enum ? ((Enum<?>) value).name() : String.valueOf(value);
            valid = validType(value) && holder.validate(descriptor.getPath(), value);
            if (isList(descriptor)) { list = new ConfigListDraft(); if (!valid) list.raw = draft; }
        }
        public void setDraft(String draft) {
            if (isList(descriptor) && draft.stripLeading().startsWith("{")) {
                list = ConfigListDraft.decode(draft); valid = validType(value) && holder.validate(descriptor.getPath(), value); return;
            }
            this.draft = draft;
            Object parsed = ConfigSyncToServer.decodeNetworkValue(holder, descriptor.getPath(), draft);
            valid = validType(parsed) && holder.validate(descriptor.getPath(), parsed);
            if (valid) value = parsed;
            if (isList(descriptor)) { list = new ConfigListDraft(); if (!valid) list.raw = draft; }
        }
        private boolean validType(Object value) {
            switch (descriptor.getValueType()) {
                case STRING: return value instanceof String;
                case BOOLEAN: return value instanceof Boolean;
                case INTEGER: return value instanceof Integer;
                case LONG: return value instanceof Long;
                case DOUBLE: return value instanceof Double && Double.isFinite((Double) value);
                case ENUM: return descriptor.getEnumClass() != null && descriptor.getEnumClass().isInstance(value);
                default:
                    if (!(value instanceof List)) return false;
                    try { ConfigListSpecHelper.parseNetworkList(ConfigListSpecHelper.encodeNetworkList((List<?>) value), descriptor); return true; }
                    catch (IllegalArgumentException invalid) { return false; }
            }
        }
    }
}
