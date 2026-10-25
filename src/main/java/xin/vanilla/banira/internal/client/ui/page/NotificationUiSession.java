package xin.vanilla.banira.internal.client.ui.page;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.common.enums.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.*;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.notification.*;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.internal.client.ConfigEditorNotifier;
import xin.vanilla.banira.internal.client.ui.*;
import xin.vanilla.banira.internal.config.ClientConfig;

import java.util.*;
import java.util.function.Consumer;

public final class NotificationUiSession implements UiPageSession, UiCloseGuard {
    public interface HistoryPort {
        List<NotificationLogEntry> entries();
        long revision();
        int unread();
        boolean markRead(long id);
        boolean markAllRead();
    }
    public interface SettingsPort {
        Map<String, NotificationTypeSettingsStore.TypeSettings> snapshot();
        List<String> knownTypes();
        void save(Map<String, NotificationTypeSettingsStore.TypeSettings> values);
    }
    public interface PositionPort {
        double x();
        double y();
        void save(double x, double y);
    }
    private final HistoryPort history;
    private final SettingsPort settings;
    private final PositionPort position;
    private Map<String, NotificationTypeSettingsStore.TypeSettings> baseline, draft;
    private final Map<String, String> durationDrafts = new HashMap<>();
    private List<NotificationLogEntry> entries = Collections.emptyList();
    private long revision = Long.MIN_VALUE, selectedId;
    private String query = "";
    private boolean historyDetailOpen;
    private int historyRowHeight = 48;
    private double x, y, originalX, originalY;
    private UiView view;
    private UiOwnedResources bindings = new UiOwnedResources();
    private final Map<UiView, UiOwnedResources> rows = new IdentityHashMap<>();
    private BaniraScreen nativeScreen;
    private UiPageSpec spec;
    private boolean closed, saving, discard;
    private String saveFailure = "";
    private boolean dragging;
    private double grabX, grabY;

    public NotificationUiSession(HistoryPort port) {
        history = Objects.requireNonNull(port); settings = null; position = null;
    }
    public NotificationUiSession(SettingsPort port) {
        history = null; settings = Objects.requireNonNull(port); position = null;
        baseline = copy(port.snapshot()); draft = copy(baseline);
    }
    public NotificationUiSession(PositionPort port) {
        history = null; settings = null; position = Objects.requireNonNull(port);
        x = originalX = finite(port.x()); y = originalY = finite(port.y());
    }
    public static NotificationUiSession history() {
        return new NotificationUiSession(new HistoryPort() {
            public List<NotificationLogEntry> entries() { return NotificationManager.get().getLog(); }
            public long revision() { return NotificationManager.get().historyRevision(); }
            public int unread() { return NotificationManager.get().unreadCount(); }
            public boolean markRead(long id) { return NotificationManager.get().markRead(id); }
            public boolean markAllRead() { return NotificationManager.get().markAllRead(); }
        });
    }
    public static NotificationUiSession types() {
        return new NotificationUiSession(new SettingsPort() {
            public Map<String, NotificationTypeSettingsStore.TypeSettings> snapshot() { return NotificationTypeSettingsStore.get().snapshot(); }
            public List<String> knownTypes() { return NotificationTypeRegistry.knownTypesSorted(); }
            public void save(Map<String, NotificationTypeSettingsStore.TypeSettings> values) { NotificationTypeSettingsStore.get().replaceAllAndSave(values); }
        });
    }
    public static NotificationUiSession position() {
        return new NotificationUiSession(new PositionPort() {
            public double x() { return ClientConfig.get().notificationHud().x(); }
            public double y() { return ClientConfig.get().notificationHud().y(); }
            public void save(double x, double y) {
                ClientConfig.get().notificationHud().x(x).y(y);
                BaniraConfigs.save(ClientConfig.class);
            }
        });
    }
    public void attachNative(BaniraScreen screen) { nativeScreen = screen; }
    public UiPageSpec spec() {
        String surface = history != null ? "notification-log" : settings != null ? "notification-types" : "hud-position";
        if (spec == null) spec = new UiPageSpec("banira_codex:" + surface, "banira_codex/ui/" + surface + ".html",
                this, previous -> nativeScreen, nativeScreen::getEffectiveTheme);
        return spec;
    }
    public String query() { return query; }
    public void query(String text) {
        if (closed) return;
        String next = text == null ? "" : text;
        if (!query.equals(next) && view != null && view.state() != null && (history != null || settings != null)) {
            UiPageState state = view.state();
            state.rowPosition(0); state.scrollOffset(0); state.scrollAnchor("");
            if (settings != null) state.selectedId("");
        }
        query = next;
        if (history != null && history.entries().stream().noneMatch(entry -> entry.id() == selectedId && matches(entry, query))) selectedId = 0;
        rebuild();
    }
    public long selectedId() { return selectedId; }
    public void restoreSelection(long id) { if (!closed) { selectedId = id; historyDetailOpen = id != 0; updateDetail(); } }
    public void select(long id) {
        if (closed || history == null || history.entries().stream().noneMatch(entry -> entry.id() == id)) return;
        if (selectedId != id && view instanceof NativeUiView) ((NativeUiView) view).state().detailScroll(0);
        selectedId = id;
        historyDetailOpen = true;
        history.markRead(id);
        revision = history.revision();
        rebuild();
    }
    public void markAllRead() {
        if (!closed && history != null && history.unread() > 0) { history.markAllRead(); refresh(); }
    }
    public List<NotificationLogEntry> filteredEntries() {
        if (history == null) return Collections.emptyList();
        List<NotificationLogEntry> result = new ArrayList<>();
        for (NotificationLogEntry entry : history.entries()) if (matches(entry, query)) result.add(entry);
        return result;
    }
    public static boolean matches(NotificationLogEntry entry, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String text = entry.component().toString().replace('\r', ' ').replace('\n', ' ') + '\n' + entry.source() + '\n' + entry.notificationType() + '\n'
                + entry.style().name() + '\n' + entry.positionName() + '\n' + entry.animationName() + '\n'
                + entry.durationTime() + '\n' + DateUtils.toDateTimeString(new Date(entry.timestamp()));
        return text.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }
    public NotificationTypeSettingsStore.TypeSettings settings(String type) {
        return NotificationTypeSettingsStore.copyOf(draft.getOrDefault(type, new NotificationTypeSettingsStore.TypeSettings()));
    }
    public void settings(String type, NotificationTypeSettingsStore.TypeSettings value) {
        if (!closed) {
            if (settings(type).durationMs() != Objects.requireNonNull(value).durationMs()) durationDrafts.remove(type);
            draft.put(type, NotificationTypeSettingsStore.copyOf(value));
            updateTypeStatus();
        }
    }
    public String durationDraft(String type) { return durationDrafts.get(type); }
    public boolean duration(String type, String text) {
        if (closed) return false;
        try {
            long value = Long.parseLong(text);
            if (value < 0) throw new NumberFormatException();
            durationDrafts.remove(type); settings(type, settings(type).durationMs(value)); return true;
        } catch (NumberFormatException invalid) { durationDrafts.put(type, text); updateTypeStatus(); return false; }
    }
    public int changedCount() {
        if (settings == null) return position != null && (x != originalX || y != originalY) ? 1 : 0;
        Set<String> keys = new HashSet<>(baseline.keySet()); keys.addAll(draft.keySet());
        int count = 0;
        for (String key : keys) if (!Objects.equals(baseline.getOrDefault(key, new NotificationTypeSettingsStore.TypeSettings()),
                draft.getOrDefault(key, new NotificationTypeSettingsStore.TypeSettings()))) count++;
        return count + (int) durationDrafts.keySet().stream().filter(key -> Objects.equals(baseline.getOrDefault(key,
                new NotificationTypeSettingsStore.TypeSettings()), draft.getOrDefault(key, new NotificationTypeSettingsStore.TypeSettings()))).count();
    }
    private static Map<String, NotificationTypeSettingsStore.TypeSettings> copy(Map<String, NotificationTypeSettingsStore.TypeSettings> values) {
        Map<String, NotificationTypeSettingsStore.TypeSettings> result = new LinkedHashMap<>();
        values.forEach((key, value) -> result.put(key, NotificationTypeSettingsStore.copyOf(value)));
        return result;
    }
    private static double finite(double value) { return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
    public double x() { return x; }
    public double y() { return y; }
    public void moveTo(double x, double y) {
        if (!closed && Double.isFinite(x) && Double.isFinite(y)) { this.x = finite(x); this.y = finite(y); updatePosition(); }
    }
    public boolean save() {
        if (closed || saving || history != null) return false;
        if (!durationDrafts.isEmpty()) return false;
        saving = true;
        saveFailure = "";
        try {
            if (settings != null) { settings.save(copy(draft)); baseline = copy(draft); }
            else { position.save(x, y); originalX = x; originalY = y; }
            return true;
        } catch (RuntimeException failure) { saveFailure = Objects.toString(failure.getMessage(), failure.getClass().getSimpleName()); return false; }
        finally { saving = false; updateTypeStatus(); updatePositionStatus(); }
    }
    public boolean saveWithFeedback() {
        boolean saved = save();
        if (settings != null) {
            if (saved) ConfigEditorNotifier.show("config_editor_save_success", 2000);
            else ConfigEditorNotifier.show("config_editor_save_failed", 4000,
                    durationDrafts.isEmpty() ? saveFailure : tr("config_editor_validation_failed").toString());
        }
        return saved;
    }
    @Override public boolean canClose(UiCloseReason reason) { return reason != UiCloseReason.USER_CLOSE || discard || changedCount() == 0; }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        if (closed) return;
        this.view = Objects.requireNonNull(view); bindings = new UiOwnedResources();
        if (view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof BaniraScreen) return;
        view.text("title", tr(history != null ? "notification_log_title" : settings != null ? "notification_type_config_title" : "notification_hud_position"));
        view.text("close", tr("config_editor_close"));
        bindings.own(view.onAction("close", () -> { if (this.view == view) { discard = true; view.requestClose(); } }));
        if (history != null || settings != null) {
            view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, 32767, "", false));
            view.draft("search", query);
            bindings.own(view.onDraft("search", value -> { if (this.view == view) query(value); }));
        }
        if (history != null) {
            view.gameElement("brand", UiModuleHeaderIcons.resolve("banira_codex", xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().allEntriesInOrder()));
            view.text("module_name", BaniraComponent.get().literal("BANIRA CODEX"));
            view.text("search_hint", tr("notification_log_search_hint"));
            view.tooltip("search", tr("notification_log_search_hint"));
            view.gameElement("mark_all_read", UiGameElement.preset(xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.CHECK));
            view.tooltip("mark_all_read", tr("notification_log_mark_all_read"));
            bindings.own(view.onAction("mark_all_read", () -> { if (this.view == view) markAllRead(); }));
            view.gameElement("types", UiGameElement.preset(xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.SETTINGS));
            view.tooltip("types", tr("notification_type_config_open"));
            bindings.own(view.onAction("types", () -> {
                if (this.view == view) Minecraft.getInstance().setScreen(new NotificationTypeConfigScreen(new NotificationTypeConfigScreen.Args().parentScreen(nativeScreen)));
            }));
            view.gameElement("position", UiGameElement.preset(xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.MOVE));
            view.tooltip("position", tr("notification_hud_position"));
            bindings.own(view.onAction("position", () -> {
                if (this.view == view) Minecraft.getInstance().setScreen(new NotificationHudPositionScreen(nativeScreen));
            }));
            view.text("clear_search", BaniraComponent.get().literal("\u00d7"));
            view.tooltip("clear_search", tr("config_editor_clear_search"));
            bindings.own(view.onAction("clear_search", () -> {
                if (this.view != view) return;
                view.draft("search", ""); query("");
            }));
            view.gameElement("back", UiGameElement.preset(xin.vanilla.banira.client.gui.widget.ButtonWidget.PresetStyle.ARROW_LEFT));
            view.tooltip("back", tr("notification_log_back"));
            bindings.own(view.onAction("back", () -> {
                if (this.view != view) return;
                historyDetailOpen = false; updateDetail();
            }));
            view.text("footer_close", tr("config_editor_close"));
            bindings.own(view.onAction("footer_close", () -> { if (this.view == view) view.requestClose(); }));
            revision = Long.MIN_VALUE; refresh();
        } else {
            view.text("save", tr("config_editor_save"));
            bindings.own(view.onAction("save", () -> {
                if (this.view != view) return;
                boolean saved = saveWithFeedback();
                if (saved && position != null) view.requestClose();
            }));
            if (settings != null) {
                view.gameElement("brand", UiModuleHeaderIcons.resolve("banira_codex", xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().allEntriesInOrder()));
                view.text("module_name", tr("mod_name"));
                view.text("origin", tr("config_editor_client_scope"));
                view.text("category_title", tr("config_editor_categories"));
                view.text("search_hint", tr("notification_type_config_search_hint"));
                view.tooltip("search", tr("notification_type_config_search_hint"));
                view.text("clear_search", BaniraComponent.get().literal("\u00d7"));
                view.tooltip("clear_search", tr("config_editor_clear_search"));
                bindings.own(view.onAction("clear_search", () -> {
                    if (this.view != view) return;
                    view.draft("search", ""); query("");
                }));
                view.text("cancel", tr("config_editor_close"));
                bindings.own(view.onAction("cancel", () -> { if (this.view == view) { discard = true; view.requestClose(); } }));
                rebuildTypes();
            } else bindPosition();
        }
    }
    private static Component tr(String key) { return BaniraComponent.get().transClientAuto(key); }
    private void releaseRows() { rows.values().forEach(UiOwnedResources::close); rows.clear(); }
    private void refresh() {
        if (history == null || closed || revision == history.revision()) return;
        revision = history.revision(); rebuild();
    }
    private void rebuild() {
        if (view == null || closed) return;
        if (history == null) { if (settings != null) rebuildTypes(); return; }
        releaseRows(); entries = filteredEntries();
        if (entries.stream().noneMatch(entry -> entry.id() == selectedId)) selectedId = 0;
        UiView owner = view;
        List<NotificationLogEntry> snapshot = new ArrayList<>(entries);
        final int rowHeight = preferredHistoryRowHeight();
        historyRowHeight = rowHeight;
        if (view instanceof NativeUiView) {
            UiViewState state = ((NativeUiView) view).state();
            String anchor = state.scrollAnchor();
            for (int i = 0; i < snapshot.size(); i++) if (anchor.equals("notification:" + snapshot.get(i).id())) {
                state.rowPosition(i + state.rowPosition() % 1); break;
            }
        }
        view.rows("fields", new UiRowPresentation() {
            public int rowHeight() { return rowHeight; }
            public String rowClass(int index) {
                NotificationLogEntry entry = snapshot.get(index);
                return "log-row log-" + entry.style().name().toLowerCase(Locale.ROOT) + (rowHeight == 24 ? " compact" : "")
                        + (entry.read() ? "" : " unread") + (entry.id() == selectedId ? " selected" : "");
            }
            public int size() { return snapshot.size(); }
            public String key(int index) { return "notification:" + snapshot.get(index).id(); }
            public void bind(UiView row, int index) {
                NotificationLogEntry entry = snapshot.get(index);
                UiOwnedResources previous = rows.remove(row); if (previous != null) previous.close();
                UiOwnedResources owned = new UiOwnedResources(); rows.put(row, owned);
                String summary = net.minecraft.ChatFormatting.stripFormatting(entry.component().toString()).replace('\n', ' ').replace('\r', ' ');
                row.text("open", BaniraComponent.get().literal((entry.read() ? "" : "\u2022 ") + summary));
                row.tooltip("open", entry.component());
                row.text("label", BaniraComponent.get().literal(moduleName(entry) + " / " + DateUtils.toDateTimeString(new Date(entry.timestamp()))));
                owned.own(row.onAction("open", () -> { if (view == owner) select(entry.id()); }));
            }
            public void unbind(UiView row, int index) { UiOwnedResources owned = rows.remove(row); if (owned != null) owned.close(); }
        }, 120);
        view.enabled("mark_all_read", history.unread() > 0);
        view.text("unread_count", BaniraComponent.get().literal(tr("notification_log_unread") + ": " + history.unread()));
        view.text("list_count", BaniraComponent.get().literal(entries.size() + " / " + history.entries().size()));
        view.text("empty", tr(history.entries().isEmpty() ? "notification_log_empty" : "notification_log_no_results"));
        view.enabled("empty", entries.isEmpty());
        updateDetail();
    }
    private void updateDetail() {
        if (view == null || history == null) return;
        NotificationLogEntry selected = history.entries().stream().filter(entry -> entry.id() == selectedId).findFirst().orElse(null);
        if (selected == null) { selectedId = 0; historyDetailOpen = false; }
        view.value("history_detail", UiValue.bool(historyDetailOpen));
        view.enabled("history_detail_content", selected != null);
        view.text("select_hint", tr("notification_log_select_hint"));
        view.enabled("select_hint", selected == null);
        view.text("text", selected == null ? BaniraComponent.get().literal("") : selected.component());
        String[] keys = {"source", "type", "style", "position", "animation", "duration"};
        String[] values = selected == null ? new String[6] : new String[]{sourceName(selected), Objects.toString(selected.notificationType(), ""),
                selected.style().name(), Objects.toString(selected.positionName(), ""), Objects.toString(selected.animationName(), ""), selected.durationTime() + " ms"};
        for (int i = 0; i < keys.length; i++) {
            Component value = BaniraComponent.get().literal(selected == null ? "" : tr("notification_log_meta_" + keys[i]) + ": " + values[i]);
            view.text("meta_" + keys[i], value); view.tooltip("meta_" + keys[i], value);
        }
        view.text("detail_type", BaniraComponent.get().literal(selected == null ? "" : moduleName(selected)));
        view.tooltip("detail_type", BaniraComponent.get().literal(selected == null ? "" : Objects.toString(selected.notificationType(), "")));
        view.text("detail_time", BaniraComponent.get().literal(selected == null ? "" : DateUtils.toDateTimeString(new Date(selected.timestamp()))));
        view.text("read_status", selected == null ? BaniraComponent.get().empty() : tr(selected.read() ? "notification_log_read" : "notification_log_unread"));
        if (selected == null) { view.text("metadata", BaniraComponent.get().literal("")); return; }
        String metadata = DateUtils.toDateTimeString(new Date(selected.timestamp())) + "\n"
                + tr("notification_log_meta_source") + ": " + Objects.toString(selected.source(), "") + "\n"
                + tr("notification_log_meta_type") + ": " + Objects.toString(selected.notificationType(), "") + "\n"
                + tr("notification_log_meta_style") + ": " + selected.style().name() + "\n"
                + tr("notification_log_meta_position") + ": " + Objects.toString(selected.positionName(), "") + "\n"
                + tr("notification_log_meta_animation") + ": " + Objects.toString(selected.animationName(), "") + "\n"
                + tr("notification_log_meta_duration") + ": " + selected.durationTime() + " ms";
        view.text("metadata", BaniraComponent.get().literal(metadata));
        view.tooltip("metadata", BaniraComponent.get().literal(metadata));
    }
    private static String sourceName(NotificationLogEntry entry) {
        return "network".equals(entry.source()) ? tr("notification_log_source_network").toString()
                : "local".equals(entry.source()) ? tr("notification_log_source_local").toString() : Objects.toString(entry.source(), "");
    }
    private int preferredHistoryRowHeight() {
        double height = view == null ? 0 : view.bounds("content").height();
        return height > 0 && height < 192 ? 24 : 48;
    }
    private static String moduleName(NotificationLogEntry entry) {
        String type = Objects.toString(entry.notificationType(), "default");
        String namespace = type.split("[.:]", 2)[0];
        Component display = NotificationTypeRegistry.modDisplayNameInternal(namespace);
        return display == null ? namespace : display.toString();
    }
    private void rebuildTypes() {
        releaseRows();
        List<String> types = new ArrayList<>(new TreeSet<>(settings.knownTypes()));
        types.addAll(draft.keySet().stream().filter(type -> !types.contains(type)).sorted().toList());
        int total = types.size();
        xin.vanilla.banira.client.gui.search.ConfigSearchQuery search = xin.vanilla.banira.client.gui.search.ConfigSearchQuery.of(query);
        types.removeIf(type -> !search.matches(type, Objects.toString(NotificationTypeRegistry.tooltipInternal(type), ""),
                Objects.toString(NotificationTypeRegistry.modDisplayNameInternal(typeNamespace(type)), "")));
        UiView owner = view;
        view.rows("fields", new UiRowPresentation() {
            public int rowHeight() { return 24; }
            public String rowClass(int index) { return "player-config-field"; }
            public int size() { return types.size() * 5; }
            public String key(int index) { return "type:" + types.get(index / 5) + ":" + index % 5; }
            public void bind(UiView row, int index) { bindType(row, types.get(index / 5), index % 5, owner); }
            public void unbind(UiView row, int index) { UiOwnedResources owned = rows.remove(row); if (owned != null) owned.close(); }
        }, 120);
        bindTypeNavigation(types, owner);
        view.text("breadcrumb", tr("notification_type_config_root_title"));
        view.tooltip("breadcrumb", tr("notification_type_config_root_title"));
        view.text("entry_count", BaniraComponent.get().literal(types.size() + " / " + total));
        view.text("category_count", BaniraComponent.get().literal(Integer.toString(types.size())));
        view.text("empty", tr("config_editor_empty"));
        view.enabled("empty", types.isEmpty());
        updateTypeStatus();
    }
    private static String typeNamespace(String type) {
        return "default".equals(type) ? "banira_codex" : type.split("[.:]", 2)[0];
    }
    private void bindTypeNavigation(List<String> types, UiView owner) {
        Map<String, Integer> anchors = new LinkedHashMap<>();
        anchors.put("all", 0);
        for (int index = 0; index < types.size(); index++) {
            anchors.putIfAbsent("module:" + typeNamespace(types.get(index)), index * 5);
            anchors.put("type:" + types.get(index), index * 5);
        }
        List<String> keys = new ArrayList<>(anchors.keySet());
        view.rows("categories", new UiRowPresentation() {
            public int rowHeight() { return 20; }
            public String scrollerId() { return "category_content"; }
            public boolean tracksPageScroll() { return false; }
            public boolean textTools() { return true; }
            public String rowClass(int index) {
                String selected = owner.state() == null ? "" : owner.state().selectedId();
                return "config-navigation" + (keys.get(index).equals(selected.isEmpty() ? "all" : selected) ? " config-selected" : "");
            }
            public int size() { return keys.size(); }
            public String key(int index) { return keys.get(index); }
            public void bind(UiView row, int index) {
                if (view != owner) return;
                UiOwnedResources previous = rows.remove(row); if (previous != null) previous.close();
                UiOwnedResources owned = new UiOwnedResources(); rows.put(row, owned);
                String key = keys.get(index);
                boolean module = key.startsWith("module:"), type = key.startsWith("type:");
                String id = key.equals("all") ? "" : key.substring(key.indexOf(':') + 1);
                Component label = key.equals("all") ? tr("notification_type_config_root_title")
                        : module ? NotificationTypeRegistry.modDisplayNameInternal(id) : NotificationTypeRegistry.tooltipInternal(id);
                if (label == null || label.isEmpty()) label = tr(module ? "notification_log_meta_source" : "notification_log_meta_type");
                row.text("open", type ? BaniraComponent.get().literal("  ").append(label) : label);
                row.tooltip("open", BaniraComponent.get().literal(id).append(id.isEmpty() ? "" : "\n").append(label));
                long count = type ? 1 : module ? types.stream().filter(value -> typeNamespace(value).equals(id)).count() : types.size();
                row.text("count", BaniraComponent.get().literal(Long.toString(count)));
                owned.own(row.onAction("open", () -> {
                    if (view != owner || owner.state() == null) return;
                    int target = anchors.get(key);
                    owner.state().selection("", 0, 0); owner.state().selectedId(key);
                    owner.state().rowPosition(target); owner.state().scrollOffset(target * 24.0);
                    owner.state().scrollAnchor(types.isEmpty() ? "" : "type:" + types.get(target / 5) + ":0");
                    rebuildTypes();
                }));
            }
        }, 120);
    }
    private void updateTypeStatus() {
        if (settings == null || view == null || closed) return;
        int changed = changedCount();
        view.text("status", !durationDrafts.isEmpty() ? BaniraComponent.get().transClientAuto("player_config_invalid_status", durationDrafts.size())
                : changed == 0 ? tr("config_editor_clean_status") : BaniraComponent.get().transClientAuto("config_editor_pending_status", changed));
        Component error = !durationDrafts.isEmpty() ? tr("config_editor_validation_failed").append(": " + String.join(", ", new TreeSet<>(durationDrafts.keySet())))
                : BaniraComponent.get().literal(saveFailure);
        view.text("error", error); view.tooltip("error", error); view.enabled("error", !error.isEmpty());
    }
    private void bindType(UiView row, String type, int field, UiView owner) {
        UiOwnedResources previous = rows.remove(row); if (previous != null) previous.close();
        UiOwnedResources owned = new UiOwnedResources(); rows.put(row, owned);
        String key = new String[]{"hidden", "duration", "animation", "position", "display"}[field];
        row.text("label", tr("notification_type_config_" + key));
        row.text("field_unit", BaniraComponent.get().literal(field == 1 ? "ms" : ""));
        Component tooltip = NotificationTypeRegistry.tooltipInternal(type);
        Component subtitle = tooltip == null || tooltip.isEmpty() ? NotificationTypeRegistry.modDisplayNameInternal(typeNamespace(type)) : tooltip;
        row.text("key", subtitle == null || subtitle.isEmpty() ? tr("notification_log_meta_type") : subtitle);
        Component fieldDescription = BaniraComponent.get().literal(type + " / " + tr("notification_type_config_" + key))
                .append(tooltip == null ? "" : "\n", tooltip == null ? "" : tooltip);
        row.text("tooltip", fieldDescription);
        row.tooltip("key", fieldDescription);
        row.tooltip("input", fieldDescription);
        NotificationTypeSettingsStore.TypeSettings value = settings(type);
        Consumer<NotificationTypeSettingsStore.TypeSettings> update = next -> { if (view == owner) settings(type, next); };
        if (field == 0) {
            row.value("input", UiValue.bool(value.hidden()));
            owned.own(row.onValue("input", next -> update.accept(settings(type).hidden(next.asBoolean()))));
        } else if (field == 1) {
            row.control("input", new UiControlSpec(UiControlSpec.Kind.DECIMAL, 19, "", false));
            row.draft("input", durationDrafts.getOrDefault(type, Long.toString(value.durationMs())));
            if (durationDrafts.containsKey(type)) {
                row.text("validation", tr("config_editor_validation_failed"));
                row.tooltip("input", tr("config_editor_validation_failed"));
            }
            owned.own(row.onDraft("input", text -> {
                if (view != owner) return;
                boolean valid = duration(type, text);
                row.text("validation", valid ? BaniraComponent.get().literal("") : tr("config_editor_validation_failed"));
                row.tooltip("input", valid ? fieldDescription : tr("config_editor_validation_failed"));
            }));
        } else {
            List<DropdownOption> options = new ArrayList<>();
            if (field != 4) options.add(new DropdownOption("", tr("notification_type_config_inherit").toString()));
            Enum<?>[] values = field == 2 ? EnumMoveType.values() : field == 3 ? EnumPosition.values() : EnumNotificationTypeDisplayMode.values();
            for (Enum<?> item : values) {
                Component description = item instanceof IEnumDescribable ? ((IEnumDescribable) item).enumDescription() : null;
                options.add(description == null || description.isEmpty() ? new DropdownOption(item.name())
                        : new DropdownOption(item.name(), description.toString(), net.minecraft.world.item.ItemStack.EMPTY, null, description.clone()));
            }
            row.control("input", new UiControlSpec(UiControlSpec.Kind.SELECT, 256, "", false, options, DropdownInputMode.SELECTION_ONLY));
            row.value("input", UiValue.text(field == 2 ? value.animationName() : field == 3 ? value.positionName() : value.displayMode().name()));
            owned.own(row.onValue("input", next -> {
                NotificationTypeSettingsStore.TypeSettings changed = settings(type);
                if (field == 2) changed.animationName(next.asText());
                else if (field == 3) changed.positionName(next.asText());
                else changed.displayMode(EnumNotificationTypeDisplayMode.valueOf(next.asText()));
                update.accept(changed);
            }));
        }
    }
    private void bindPosition() {
        view.gameElement("brand", UiModuleHeaderIcons.resolve("banira_codex", xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().allEntriesInOrder()));
        view.text("module_name", tr("mod_name"));
        view.text("origin", tr("config_editor_client_scope"));
        view.text("cancel", tr("cancel"));
        updatePosition();
        view.tooltip("preview", tr("notification_hud_drag"));
        UiView owner = view;
        bindings.own(view.onAction("cancel", () -> { if (view == owner) { discard = true; owner.requestClose(); } }));
        bindings.own(view.onPointer("preview", event -> {
            if (view != owner || !event.finite()) return;
            if (event.kind() == UiPointer.Kind.UP) { if (event.button() == 0) dragging = false; return; }
            ScreenCoordinate area = view.bounds("viewport"), preview = view.bounds("preview");
            if (event.kind() == UiPointer.Kind.DOWN) {
                if (event.button() != 0 || event.x() < preview.x() || event.x() >= preview.x() + preview.width()
                        || event.y() < preview.y() || event.y() >= preview.y() + preview.height()) return;
                dragging = true; grabX = event.x() - preview.x(); grabY = event.y() - preview.y();
            } else if (dragging) moveTo(NotificationHudState.relative(event.x() - grabX - area.x(), (int) area.width(), NotificationUnreadHud.WIDTH),
                    NotificationHudState.relative(event.y() - grabY - area.y(), (int) area.height(), NotificationUnreadHud.HEIGHT));
        }));
    }
    private void updatePosition() {
        if (view != null && position != null) { view.hudPosition(x, y); updatePositionStatus(); }
    }
    private void updatePositionStatus() {
        if (view == null || position == null || view.nativeView()) return;
        view.text("status", changedCount() == 0 ? tr("config_editor_clean_status")
                : BaniraComponent.get().transClientAuto("config_editor_pending_status", changedCount()));
        Component error = saveFailure.isEmpty() ? BaniraComponent.get().empty()
                : BaniraComponent.get().transClientAuto("config_editor_save_failed", saveFailure);
        view.text("error", error); view.tooltip("error", error); view.enabled("error", !error.isEmpty());
    }
    @Override public void tick() {
        refresh();
        if (history != null && view != null && !view.nativeView() && !closed && historyRowHeight != preferredHistoryRowHeight()) rebuild();
    }
    @Override public void detach(UiDetachReason reason) {
        dragging = false; view = null; bindings.close(); releaseRows();
    }
    @Override public void close(UiCloseReason reason) {
        if (closed) return;
        closed = true; detach(UiDetachReason.DISCONNECT);
    }
}
