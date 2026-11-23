package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.internal.client.ui.*;
import xin.vanilla.banira.internal.client.ui.page.*;

import java.util.*;
import java.util.function.Consumer;

final class QuickActionScreenPicker extends BaniraScreen implements UiNativePage {
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

        private PickerAction(String id, String label, String tooltip, boolean group, boolean expanded, boolean enabled, Runnable open) {
            super(id, BaniraComponent.get().literal(label), open, null, enabled);
            this.group = group;
            this.expanded = expanded;
            this.tooltip = tooltip;
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

    private final class PickerSession extends QuickActionEditUiSession {
        private UiView pickerView;

        private PickerSession() {
            super(BaniraComponent.get().transClientAuto("quick_screen_title"), Collections::emptyList, Collections.emptyList());
            searchHint(BaniraComponent.get().transClientAuto("quick_screen_search"));
        }

        @Override
        public void bind(UiView view) {
            super.bind(view);
            if (!boundTo(view) || view.nativeView()) return;
            pickerView = view;
            updateChrome(view);
            view.text("footer_close", BaniraComponent.get().transClientAuto("cancel"));
            ownBinding(view.onAction("filter_unavailable", () -> {
                if (boundTo(view)) toggleFilter();
            }));
            view.text("confirm", BaniraComponent.get().transClientAuto("confirm"));
            ownBinding(view.onAction("confirm", () -> {
                if (boundTo(view)) confirm();
            }));
        }

        @Override
        public void detach(UiDetachReason reason) {
            pickerView = null;
            super.detach(reason);
        }

        private void update(boolean resetScroll) {
            refreshList(resetScroll);
            if (pickerView != null) updateChrome(pickerView);
        }

        @Override
        public void query(String text) {
            super.query(text);
            if (pickerView != null) updateChrome(pickerView);
        }

        private void updateChrome(UiView view) {
            view.text("filter_unavailable", BaniraComponent.get().transClientAuto(filterKey()));
            view.tooltip("filter_unavailable", BaniraComponent.get().transClientAuto(filterHintKey()));
            String status = scanStatusKey();
            view.text("status", status.isEmpty() ? BaniraComponent.get().literal("") : BaniraComponent.get().transClientAuto(status));
            view.enabled("confirm", groups.selection() != null);
        }

        @Override
        protected List<Action> visibleActions() {
            return new ArrayList<>(QuickActionScreenPicker.this.actions(query()));
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
                    + (selected(picker) ? " config-selected" : "");
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
        ButtonWidget close = new ButtonWidget(this);
        close.id("close");
        close.bounds(new ScreenCoordinate(LEFT, footerY, footerButtonWidth, ROW_HEIGHT));
        close.text(CustomQuickActionEditor.t("cancel").content());
        close.onClick(button -> onClose());
        addWidget(close);
        confirmButton = new ButtonWidget(this);
        confirmButton.id("confirm");
        confirmButton.bounds(new ScreenCoordinate(width - LEFT - footerButtonWidth, footerY, footerButtonWidth, ROW_HEIGHT));
        confirmButton.text(CustomQuickActionEditor.t("confirm").content());
        confirmButton.onClick(button -> confirm());
        addWidget(confirmButton);
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
    protected void onRender(MatrixStack graphics, float partialTicks) {
        AbstractGuiUtils.drawRoundedRect(graphics, 16, 16, width - 32, height - 32, 6, 6, 6, 6, getEffectiveTheme().bgSurface());
        String title = CustomQuickActionEditor.t("quick_screen_title").content();
        font.draw(graphics, title, (width - font.width(title)) / 2f, 27, getEffectiveTheme().textPrimary());
        String status = scanStatusKey();
        if (!status.isEmpty() && confirmButton != null) {
            int available = (int) confirmButton.x() * 2 - width - 12;
            if (available >= font.width("...")) {
                String message = QuickActionTextLayout.ellipsize(CustomQuickActionEditor.t(status).content(), available, font::width);
                font.draw(graphics, message, (width - font.width(message)) / 2f, height - FOOTER_HEIGHT + 6, getEffectiveTheme().textSecondary());
            }
        }
        if (!loading && !failed && nativeRows.isEmpty()) {
            font.draw(graphics, CustomQuickActionEditor.t("quick_screen_empty").content(), LEFT, LIST_Y + 5, getEffectiveTheme().textSecondary());
        }
        for (int i = 0; i < buttons.size() && offset + i < nativeRows.size(); i++) {
            if (!nativeRows.get(offset + i).group) {
                int y = LIST_Y + i * ROW_STRIDE - (i == 0 ? 0 : ROW_STRIDE - ROW_HEIGHT);
                AbstractGuiUtils.fill(graphics, LEFT, y, 1, ROW_STRIDE, getEffectiveTheme().textHint());
            }
        }
        super.renderWidgets(graphics, partialTicks);
    }
}
