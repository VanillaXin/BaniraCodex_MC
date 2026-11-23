package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseClickTracker;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap;
import xin.vanilla.banira.internal.client.ui.*;
import xin.vanilla.banira.internal.client.ui.page.*;

import java.util.*;
import java.util.function.Consumer;

final class QuickActionScreenPicker extends BaniraScreen implements UiNativePage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson SCRIPT_JSON = new Gson();
    private static final String SCRIPT_CHANNEL = "banira_picker_script";
    private static final int LEFT = 26;
    private static final int SEARCH_Y = 45;
    private static final int LIST_Y = 73;
    private static final int ROW_HEIGHT = CollapsiblePanelWidget.DEFAULT_HEADER_HEIGHT;
    private static final int ROW_STRIDE = 24;
    private static final int FOOTER_HEIGHT = 46;
    private final Screen parent;
    private final Consumer<String> accepted;
    private final String initial;
    private final List<QuickActionScreenRows.Item> items = new ArrayList<>();
    private final QuickActionScreenRows groups = new QuickActionScreenRows();
    private List<PickerAction> nativeRows = Collections.emptyList();
    private boolean loading = true, failed;
    private boolean hideUnavailable;
    private ButtonWidget filterButton;
    private ButtonWidget confirmButton;
    private TooltipWidget filterTooltip;
    private int offset;
    private final List<ButtonWidget> buttons = new ArrayList<>();
    private final List<CollapsiblePanelWidget> headers = new ArrayList<>();
    private final List<TooltipWidget> tooltips = new ArrayList<>();
    private ScrollbarWidget scrollbar;
    private PickerSession session;

    QuickActionScreenPicker(Screen parent, String initial, Collection<String> registered, Consumer<String> accepted) {
        super(BaniraComponent.get().transClientAuto("quick_screen_title").toVanilla());
        this.parent = parent;
        this.initial = initial;
        this.accepted = accepted;
        previousScreen(parent);
        inheritThemeAndSeason(this, parent, null, null);
        String group = CustomQuickActionEditor.t("quick_screen_registered").content();
        for (String id : registered) {
            String name = id.substring(id.indexOf(':') + 1);
            items.add(new QuickActionScreenRows.Item("registered:" + id, group, name, id, "", () -> finish(id)));
        }
        QuickActionScreenCatalog.load().whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            failed = error != null;
            if (result != null) for (QuickActionScreenCatalog.Entry entry : result) {
                String reason = entry.constructors.isEmpty() ? CustomQuickActionEditor.t("quick_screen_context_required").content() : "";
                items.add(new QuickActionScreenRows.Item(entry.className, entry.mod, title(entry), entry.className, reason, () -> choose(entry)));
            }
            if (session != null) session.catalogDirty = true;
            refreshRows(false);
        }));
    }

    private String title(QuickActionScreenCatalog.Entry entry) {
        for (String key : entry.titleKeys) if (I18n.exists(key)) return I18n.get(key);
        return entry.className.substring(entry.className.lastIndexOf('.') + 1);
    }

    private List<PickerAction> actions(String query) {
        List<PickerAction> result = new ArrayList<>();
        for (QuickActionScreenRows.Row row : groups.rows(items, query, hideUnavailable)) {
            if (row.item == null) {
                String label = (row.expanded ? "\u25be  " : "\u25b8  ") + row.group;
                result.add(new PickerAction("@group:" + row.group, label, row.group, true, row.expanded, true, () -> {
                    groups.toggle(row.group);
                    refreshRows(false);
                }));
            } else {
                QuickActionScreenRows.Item item = row.item;
                result.add(new PickerAction(item.id, item.title, item.tooltip(), false, false, item.selectable(), () -> click(item)));
            }
        }
        return result;
    }

    private static final class PickerAction extends NavigationUiSession.Action {
        private final boolean group;
        private final boolean expanded;
        private final String tooltip;
        private final boolean highlighted;

        private PickerAction(String id, String label, String tooltip, boolean group, boolean expanded, boolean enabled, Runnable open) {
            this(id, label, tooltip, group, expanded, enabled, false, open);
        }

        private PickerAction(String id, String label, String tooltip, boolean group, boolean expanded, boolean enabled, boolean highlighted, Runnable open) {
            super(id, BaniraComponent.get().literal(label), open, null, enabled);
            this.group = group;
            this.expanded = expanded;
            this.tooltip = tooltip;
            this.highlighted = highlighted;
        }
    }

    private String filterKey() {
        return hideUnavailable ? "quick_screen_only_usable" : "quick_screen_all";
    }

    private String filterHintKey() {
        return hideUnavailable ? "quick_screen_show_unavailable" : "quick_screen_hide_unavailable";
    }

    private void toggleFilter() {
        groups.resetClick();
        hideUnavailable = !hideUnavailable;
        refreshRows(true);
    }

    private void click(QuickActionScreenRows.Item item) {
        if (groups.click(item, System.nanoTime() / 1_000_000L)) confirm();
        else refreshRows(false);
    }

    private void confirm() {
        QuickActionScreenRows.Item choice = groups.confirm();
        if (choice != null) choice.select.run();
    }

    private boolean selected(PickerAction action) {
        QuickActionScreenRows.Item choice = groups.selection();
        return choice != null && !action.group && choice.id.equals(action.id());
    }

    private String scanStatusKey() {
        return loading ? "quick_screen_loading" : failed ? "quick_screen_failed" : "";
    }

    private void refreshRows(boolean resetScroll) {
        if (resetScroll) offset = 0;
        if (session != null) session.update(resetScroll);
        if (scrollbar != null) {
            nativeRows = actions(uiSession().query());
            filterButton.text(CustomQuickActionEditor.t(filterKey()).content());
            filterTooltip.text(CustomQuickActionEditor.t(filterHintKey()));
            updateRows();
        }
    }

    private record ScriptRow(String group, String id, boolean expanded, boolean selected) {
    }

    private record ScriptProjection(JsonObject state, List<ScriptRow> rows, String confirmed, boolean selected,
                                    boolean only) {
    }

    private final class PickerSession extends QuickActionEditUiSession {
        private UiDomView pickerView;
        private final Map<String, QuickActionScreenRows.Item> catalog = new HashMap<>();
        private List<Action> projection = Collections.emptyList();
        private JsonObject checkpoint;
        private String pickerQuery = "";
        private boolean initialized, catalogDirty = true, selected, committing;
        private int waiting;

        private PickerSession() {
            super(BaniraComponent.get().transClientAuto("quick_screen_title"), Collections::emptyList, Collections.emptyList());
            searchHint(BaniraComponent.get().transClientAuto("quick_screen_search"));
        }

        @Override
        public void bind(UiView view) {
            super.bind(view);
            if (!boundTo(view) || !(view instanceof UiDomView dom)) return;
            pickerView = dom;
            view.draft("search", pickerQuery);
            updateChrome(view);
            view.text("footer_close", BaniraComponent.get().transClientAuto("cancel"));
            ownBinding(view.onAction("filter_unavailable", () -> {
                if (boundTo(view)) dispatch("filter", "", true);
            }));
            view.text("confirm", BaniraComponent.get().transClientAuto("confirm"));
            ownBinding(view.onAction("confirm", () -> {
                if (boundTo(view)) dispatch("confirm", "", false);
            }));
        }

        @Override
        public void detach(UiDetachReason reason) {
            pickerView = null;
            initialized = false;
            waiting = 0;
            selected = false;
            projection = Collections.emptyList();
            super.detach(reason);
        }

        private void update(boolean resetScroll) {
            if (initialized) dispatch("sync", "", resetScroll);
            else if (pickerView != null) updateChrome(pickerView);
        }

        @Override
        public void query(String text) {
            String next = text == null ? "" : text;
            if (pickerQuery.equals(next)) return;
            pickerQuery = next;
            update(true);
        }

        @Override
        public String query() {
            return pickerQuery;
        }

        @Override
        public void tick() {
            if (pickerView != null && !initialized && !dispatch("sync", "", false) && ++waiting >= 60) {
                fallback(new IllegalStateException("Window picker script did not initialize"));
            }
        }

        private void fallback(Throwable failure) {
            if (pickerView == null) return;
            LOGGER.warn("Window picker script failed; using the native picker", failure);
            pickerView = null;
            initialized = false;
            BaniraCodexClientBootstrap.uiSessions().fallback();
        }

        private boolean dispatch(String action, String value, boolean resetScroll) {
            UiDomView view = pickerView;
            if (view == null || !boundTo(view) || committing) return false;
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", action);
                request.addProperty("value", value);
                request.addProperty("query", pickerQuery);
                request.addProperty("now", System.nanoTime() / 1_000_000L);
                request.addProperty("doubleClick", MouseClickTracker.DEFAULT_DOUBLE_CLICK_MS);
                if (!initialized) {
                    request.addProperty("init", true);
                    request.addProperty("only", hideUnavailable);
                    if (checkpoint != null) request.add("state", checkpoint);
                }
                if (!initialized || catalogDirty) request.add("catalog", scriptCatalog());
                String json = view.exchangePageScript(SCRIPT_CHANNEL, request.toString());
                if (json == null) {
                    if (initialized) fallback(new IllegalStateException("Window picker script endpoint disappeared"));
                    return false;
                }
                ScriptProjection answer = SCRIPT_JSON.fromJson(json, ScriptProjection.class);
                if (answer == null || answer.state == null || answer.rows == null)
                    throw new IllegalStateException("Invalid picker script result");
                QuickActionScreenRows.Item choice = null;
                if (answer.confirmed != null) {
                    choice = catalog.get(answer.confirmed);
                    if (choice == null || !choice.selectable()
                            || !("confirm".equals(action) || "select".equals(action) && answer.confirmed.equals(value))) {
                        throw new IllegalStateException("Invalid window submission from picker script");
                    }
                }
                List<Action> visible = new ArrayList<>(answer.rows.size());
                for (ScriptRow row : answer.rows) visible.add(scriptAction(row));
                projection = visible;
                checkpoint = answer.state;
                selected = answer.selected;
                hideUnavailable = answer.only;
                initialized = true;
                catalogDirty = false;
                refreshList(resetScroll);
                updateChrome(view);
                if (choice != null) {
                    committing = true;
                    try {
                        choice.select.run();
                    } finally {
                        committing = false;
                    }
                }
                return true;
            } catch (RuntimeException | LinkageError failure) {
                fallback(failure);
                return false;
            }
        }

        private JsonArray scriptCatalog() {
            catalog.clear();
            JsonArray rows = new JsonArray();
            for (QuickActionScreenRows.Item item : items) {
                catalog.put(item.id, item);
                JsonObject row = new JsonObject();
                row.addProperty("id", item.id);
                row.addProperty("group", item.group);
                row.addProperty("title", item.title);
                row.addProperty("details", item.details);
                row.addProperty("selectable", item.selectable());
                rows.add(row);
            }
            return rows;
        }

        private Action scriptAction(ScriptRow row) {
            if (row.group != null) {
                String label = (row.expanded ? "\u25be  " : "\u25b8  ") + row.group;
                return new PickerAction("@group:" + row.group, label, row.group, true, row.expanded, true, false,
                        () -> dispatch("fold", row.group, false));
            }
            QuickActionScreenRows.Item item = catalog.get(row.id);
            if (item == null) throw new IllegalStateException("Unknown window in picker script result");
            return new PickerAction(item.id, item.title, item.tooltip(), false, false, item.selectable(), row.selected,
                    () -> dispatch("select", item.id, false));
        }

        private void updateChrome(UiView view) {
            view.text("filter_unavailable", BaniraComponent.get().transClientAuto(filterKey()));
            view.tooltip("filter_unavailable", BaniraComponent.get().transClientAuto(filterHintKey()));
            String status = scanStatusKey();
            view.text("status", status.isEmpty() ? BaniraComponent.get().literal("") : BaniraComponent.get().transClientAuto(status));
            view.enabled("confirm", initialized && selected);
        }

        @Override
        protected List<Action> visibleActions() {
            return projection;
        }

        @Override
        protected xin.vanilla.banira.common.data.Component emptyLabel() {
            return loading || failed ? BaniraComponent.get().literal("") : BaniraComponent.get().transClientAuto("quick_screen_empty");
        }

        @Override
        protected String entryCount() {
            return "";
        }

        @Override
        protected String rowClass(Action action) {
            PickerAction picker = (PickerAction) action;
            return "config-navigation " + (picker.group ? "screen-group" : picker.enabled() ? "screen-choice" : "screen-unavailable")
                    + (picker.highlighted ? " config-selected" : "");
        }

        @Override
        protected void bindRowDetails(UiView row, Action action) {
            String target = action.enabled() ? "open" : "label";
            if (!action.enabled()) row.text(target, action.label());
            row.tooltip(target, BaniraComponent.get().literal(((PickerAction) action).tooltip));
        }
    }

    private void finish(String value) {
        accepted.accept(value);
        if (!CommonUiPages.close(this, UiCloseReason.SUBMIT)) {
            if (session != null) session.close(UiCloseReason.SUBMIT);
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private void choose(QuickActionScreenCatalog.Entry entry) {
        if (entry.constructors.isEmpty()) return;
        if (entry.constructors.size() == 1) {
            QuickActionScreenCatalog.Constructor only = entry.constructors.get(0);
            if (only.parent || only.types.length == 0) {
                finish(only.parent ? entry.className
                        : new QuickActionScreenTarget(entry.className, only.types, new String[0]).encode());
                return;
            }
        }
        QuickActionScreenTarget prior;
        try {
            prior = QuickActionScreenTarget.decode(initial);
        } catch (RuntimeException exception) {
            prior = null;
        }
        InputFormScreen.Args args = new InputFormScreen.Args().setParentScreen(parent)
                .setHeaderTitle(Text.literal(title(entry)));
        List<DropdownOption> constructors = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < entry.constructors.size(); i++) {
            QuickActionScreenCatalog.Constructor ctor = entry.constructors.get(i);
            constructors.add(new DropdownOption(Integer.toString(i), ctor.label()));
            if (prior != null && entry.className.equals(prior.className) && Arrays.equals(ctor.types, prior.types))
                selected = i;
        }
        args.addWidget(new InputFormScreen.Widget().name("constructor").title(CustomQuickActionEditor.t("quick_screen_constructor"))
                .type(InputFormScreen.WidgetType.DROPDOWN).dropdownInputMode(DropdownInputMode.SELECTION_ONLY)
                .dropdownOptionEntries(constructors).defaultValue(Integer.toString(selected)));
        for (int c = 0; c < entry.constructors.size(); c++) {
            QuickActionScreenCatalog.Constructor ctor = entry.constructors.get(c);
            if (ctor.parent) continue;
            final String index = Integer.toString(c);
            for (int a = 0; a < ctor.types.length; a++) {
                String type = ctor.types[a];
                String value = type.equals("java.lang.String") ? "" : type.equals("char") || type.equals("java.lang.Character") ? " "
                        : type.equals("boolean") || type.equals("java.lang.Boolean") ? "false" : "0";
                if (prior != null && entry.className.equals(prior.className) && Arrays.equals(ctor.types, prior.types))
                    value = prior.arguments[a];
                InputFormScreen.Widget widget = new InputFormScreen.Widget().name("arg_" + c + "_" + a)
                        .title(Text.literal(CustomQuickActionEditor.t("quick_screen_argument").content() + " " + (a + 1) + " : " + type))
                        .defaultValue(value).maxLength(1024).allowEmpty(type.equals("java.lang.String"))
                        .visibleWhen(result -> index.equals(result.value("constructor")));
                if (type.equals("boolean") || type.equals("java.lang.Boolean"))
                    widget.type(InputFormScreen.WidgetType.DROPDOWN).dropdownInputMode(DropdownInputMode.SELECTION_ONLY)
                            .dropdownOptionEntries(Arrays.asList(new DropdownOption("true"), new DropdownOption("false")));
                args.addWidget(widget);
            }
        }
        args.setCallback(result -> {
            try {
                int index = Integer.parseInt(result.value("constructor"));
                QuickActionScreenCatalog.Constructor ctor = entry.constructors.get(index);
                if (ctor.parent) accepted.accept(entry.className);
                else {
                    String[] values = new String[ctor.types.length];
                    for (int a = 0; a < values.length; a++) {
                        values[a] = result.value("arg_" + index + "_" + a);
                        try {
                            QuickActionScreenTarget.parse(ctor.types[a], values[a]);
                        } catch (IllegalArgumentException error) {
                            result.runningResult(CustomQuickActionEditor.t("quick_screen_invalid_argument").content() + " " + (a + 1) + " (" + ctor.types[a] + ")");
                            return;
                        }
                    }
                    accepted.accept(new QuickActionScreenTarget(entry.className, ctor.types, values).encode());
                }
                result.runningResult("");
            } catch (RuntimeException error) {
                result.runningResult(error);
            }
        });
        Minecraft.getInstance().setScreen(new InputFormScreen(args));
    }

    @Override
    public String uiSurfaceId() {
        return "banira_codex:quick-screen-picker";
    }

    @Override
    public QuickActionEditUiSession uiSession() {
        if (session == null) session = new PickerSession();
        return session;
    }

    @Override
    public UiPageSpec uiPage() {
        return uiSession().spec(this, uiSurfaceId(), "banira_codex/ui/quick-screen-picker.html");
    }

    @Override
    public void onClose() {
        if (CommonUiPages.close(this, UiCloseReason.CANCEL)) return;
        if (session != null) session.close(UiCloseReason.CANCEL);
        super.onClose();
    }

    @Override
    protected void onRemoved() {
        CommonUiPages.removed(this);
        super.onRemoved();
    }

    @Override
    protected void initWidgets() {
        int filterWidth = Math.max(font.width(CustomQuickActionEditor.t("quick_screen_only_usable").content()),
                font.width(CustomQuickActionEditor.t("quick_screen_all").content())) + 14;
        int filterX = width - LEFT - filterWidth;
        int footerY = height - FOOTER_HEIGHT;
        int footerButtonWidth = Math.min(80, Math.max(1, (width - 2 * LEFT) / 3));
        InputWidget search = new InputWidget(this);
        search.id("search");
        search.bounds(new ScreenCoordinate(LEFT, SEARCH_Y, Math.max(1, filterX - LEFT - 6), ROW_HEIGHT));
        search.value(uiSession().query());
        search.text(CustomQuickActionEditor.t("quick_screen_search"));
        search.onTextChanged(value -> {
            uiSession().query(value);
            refreshRows(true);
        });
        addWidget(search);
        filterButton = new ButtonWidget(this);
        filterButton.id("filter_unavailable");
        filterButton.bounds(new ScreenCoordinate(filterX, SEARCH_Y, filterWidth, ROW_HEIGHT));
        filterButton.onClick(button -> toggleFilter());
        filterTooltip = new TooltipWidget(this, new ScreenCoordinate(0, 0, filterWidth, ROW_HEIGHT));
        filterTooltip.popupAtScreenCoords(true);
        filterButton.addChild(filterTooltip);
        addWidget(filterButton);
        confirmButton = new ButtonWidget(this);
        confirmButton.id("confirm");
        confirmButton.bounds(new ScreenCoordinate(LEFT, footerY, footerButtonWidth, ROW_HEIGHT));
        confirmButton.text(CustomQuickActionEditor.t("confirm").content());
        confirmButton.onClick(button -> confirm());
        addWidget(confirmButton);
        ButtonWidget close = new ButtonWidget(this);
        close.id("close");
        close.bounds(new ScreenCoordinate(width - LEFT - footerButtonWidth, footerY, footerButtonWidth, ROW_HEIGHT));
        close.text(CustomQuickActionEditor.t("cancel").content());
        close.onClick(button -> onClose());
        addWidget(close);
        buttons.clear();
        headers.clear();
        tooltips.clear();
        int count = Math.max(0, (footerY - 6 - LIST_Y) / ROW_STRIDE);
        for (int i = 0; i < count; i++) {
            int y = LIST_Y + i * ROW_STRIDE;
            ButtonWidget row = new ButtonWidget(this);
            row.id("screen_" + i);
            row.bounds(new ScreenCoordinate(LEFT, y, width - 62, ROW_HEIGHT));
            TooltipWidget tooltip = new TooltipWidget(this);
            tooltip.id("screen_tip_" + i);
            tooltip.popupAtScreenCoords(true);
            tooltip.bounds(new ScreenCoordinate(0, 0, width - 62, ROW_HEIGHT));
            row.addChild(tooltip);
            tooltips.add(tooltip);
            buttons.add(row);
            addWidget(row);

            // Reuse the config panel header; expanded children occupy the virtual rows below it.
            CollapsiblePanelWidget header = CollapsiblePanelWidget.createAutoHeight(this, LEFT, y, width - 62);
            header.id("group_" + i);
            header.headerHeight(ROW_HEIGHT).paddingTop(0).paddingBottom(0);
            headers.add(header);
            addWidget(header);
        }
        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.bounds(new ScreenCoordinate(width - 31, LIST_Y, 5, count * ROW_STRIDE));
        scrollbar.orientation(EnumOrientation.VERTICAL).minValue(0).scrollStep(1).visibleSize(count)
                .onValueChanged(value -> {
                    offset = (int) Math.round(value);
                    updateRows();
                });
        addWidget(scrollbar);
        refreshRows(false);
    }

    private void updateRows() {
        if (scrollbar == null) return;
        confirmButton.enabled(groups.selection() != null);
        int maxOffset = Math.max(0, nativeRows.size() - buttons.size());
        offset = Math.max(0, Math.min(offset, maxOffset));
        scrollbar.maxValue(maxOffset).value(offset);
        for (int i = 0; i < buttons.size(); i++) {
            int index = offset + i;
            PickerAction action = index < nativeRows.size() ? nativeRows.get(index) : null;
            ButtonWidget button = buttons.get(i);
            CollapsiblePanelWidget header = headers.get(i);
            boolean isGroup = action != null && action.group;
            button.visible(action != null && !isGroup);
            header.visible(isGroup);
            if (isGroup) {
                header.onExpandChanged(null);
                header.text(action.tooltip).tooltip(action.tooltip).expanded(action.expanded);
                header.bounds(new ScreenCoordinate(LEFT, LIST_Y + i * ROW_STRIDE, width - 62, ROW_HEIGHT));
                header.onExpandChanged(panel -> action.open());
            } else if (action != null) {
                int inset = CollapsiblePanelWidget.DEFAULT_CONTENT_INDENT;
                int rowWidth = width - 62 - inset;
                button.bounds(new ScreenCoordinate(LEFT + inset, LIST_Y + i * ROW_STRIDE, rowWidth, ROW_HEIGHT));
                tooltips.get(i).bounds(new ScreenCoordinate(0, 0, rowWidth, ROW_HEIGHT));
                String full = action.label().toVanilla().getString();
                int maxWidth = Math.max(0, (int) button.bounds().width() - 12);
                button.text(QuickActionTextLayout.ellipsize(full, maxWidth, font::width));
                button.textMaxWidth(maxWidth);
                tooltips.get(i).text(QuickActionTextLayout.wrap(action.tooltip, Math.max(40, Math.min(320, width - 24)), font::width));
                button.enabled(action.enabled());
                boolean marked = selected(action);
                button.bgColor(marked ? getEffectiveTheme().buttonBgFocused() : getEffectiveTheme().buttonBg());
                button.borderColor(marked ? getEffectiveTheme().borderFocused() : getEffectiveTheme().buttonBorder());
                button.hoverBorderColor(marked ? getEffectiveTheme().borderFocused() : getEffectiveTheme().buttonBorderHover());
                button.onClick(clicked -> action.open());
            }
        }
    }

    @Override
    protected void onMouseScrolled(MouseScrolledHandleArgs args) {
        if (scrollbar != null) {
            scrollbar.setValue(offset - args.delta());
            args.consumed(true);
        }
    }

    @Override
    protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        AbstractGuiUtils.drawRoundedRect(graphics.pose(), 16, 16, width - 32, height - 32, 6, 6, 6, 6, getEffectiveTheme().bgSurface());
        String title = CustomQuickActionEditor.t("quick_screen_title").content();
        graphics.drawString(font, title, (width - font.width(title)) / 2, 27, getEffectiveTheme().textPrimary(), false);
        String status = scanStatusKey();
        if (!status.isEmpty() && confirmButton != null) {
            int available = width - 2 * (int) (confirmButton.x() + confirmButton.bounds().width()) - 12;
            if (available >= font.width("...")) {
                String message = QuickActionTextLayout.ellipsize(CustomQuickActionEditor.t(status).content(), available, font::width);
                graphics.drawString(font, message, (width - font.width(message)) / 2, height - FOOTER_HEIGHT + 6, getEffectiveTheme().textSecondary(), false);
            }
        }
        if (!loading && !failed && nativeRows.isEmpty()) {
            graphics.drawString(font, CustomQuickActionEditor.t("quick_screen_empty").content(), LEFT, LIST_Y + 5, getEffectiveTheme().textSecondary(), false);
        }
        for (int i = 0; i < buttons.size() && offset + i < nativeRows.size(); i++) {
            if (!nativeRows.get(offset + i).group) {
                int y = LIST_Y + i * ROW_STRIDE - (i == 0 ? 0 : ROW_STRIDE - ROW_HEIGHT);
                AbstractGuiUtils.fill(graphics.pose(), LEFT, y, 1, ROW_STRIDE, getEffectiveTheme().textHint());
            }
        }
        super.renderWidgets(graphics, partialTicks);
    }
}
