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
    private final Screen parent;
    private final Consumer<String> accepted;
    private final String initial;
    private final List<String> registered;
    private List<QuickActionScreenCatalog.Entry> entries = Collections.emptyList();
    private boolean loading = true, failed;
    private String query = "";
    private String selectedMod = "";
    private DropdownSelectWidget modFilter;
    private int offset;
    private final List<ButtonWidget> buttons = new ArrayList<>();
    private final List<TooltipWidget> tooltips = new ArrayList<>();
    private ScrollbarWidget scrollbar;
    private QuickActionEditUiSession session;

    QuickActionScreenPicker(Screen parent, String initial, Collection<String> registered, Consumer<String> accepted) {
        super(BaniraComponent.get().transClientAuto("quick_screen_title").toVanilla());
        this.parent = parent;
        this.initial = initial;
        this.accepted = accepted;
        this.registered = new ArrayList<>(registered);
        previousScreen(parent);
        inheritThemeAndSeason(this, parent, null, null);
        QuickActionScreenCatalog.load().whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            failed = error != null;
            if (result != null) entries = result;
            if (session != null) {
                configureFilter();
                session.refresh();
            }
            if (modFilter != null) modFilter.optionEntries(modOptions());
            updateRows();
        }));
    }

    private String title(QuickActionScreenCatalog.Entry entry) {
        for (String key : entry.titleKeys) if (I18n.exists(key)) return I18n.get(key);
        return entry.className.substring(entry.className.lastIndexOf('.') + 1);
    }

    private List<NavigationUiSession.Action> actions() {
        List<NavigationUiSession.Action> result = new ArrayList<>();
        if (selectedMod.isEmpty() || selectedMod.equals("@registered")) for (String id : registered)
            result.add(action("registered:" + id, CustomQuickActionEditor.t("quick_screen_registered").content() + " / " + id, () -> finish(id)));
        for (QuickActionScreenCatalog.Entry entry : entries) {
            if (!selectedMod.isEmpty() && !selectedMod.equals(entry.mod)) continue;
            String name = entry.mod + " / " + title(entry) + " / " + entry.className;
            if (entry.constructors.isEmpty())
                name = CustomQuickActionEditor.t("quick_screen_context_required").content() + " / " + name;
            result.add(new NavigationUiSession.Action(entry.className, BaniraComponent.get().literal(name),
                    () -> choose(entry), null, !entry.constructors.isEmpty()));
        }
        return result;
    }

    private NavigationUiSession.Action action(String id, String label, Runnable action) {
        return new NavigationUiSession.Action(id, BaniraComponent.get().literal(label), action, null);
    }

    private List<DropdownOption> modOptions() {
        List<DropdownOption> options = new ArrayList<>();
        options.add(new DropdownOption("", CustomQuickActionEditor.t("quick_screen_all").content()));
        if (!registered.isEmpty())
            options.add(new DropdownOption("@registered", CustomQuickActionEditor.t("quick_screen_registered").content()));
        entries.stream().map(entry -> entry.mod).distinct().forEach(mod -> options.add(new DropdownOption(mod)));
        return options;
    }

    private void configureFilter() {
        session.icons(modOptions(), () -> selectedMod, value -> {
            selectedMod = value;
            offset = 0;
            session.refresh();
        });
        session.selectionTitle(BaniraComponent.get().transClientAuto("quick_screen_mod"));
        session.searchHint(BaniraComponent.get().transClientAuto("quick_screen_search"));
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
        if (session == null) {
            session = new QuickActionEditUiSession(BaniraComponent.get().transClientAuto("quick_screen_title"), this::actions, Collections.emptyList());
            configureFilter();
            session.context(() -> BaniraComponent.get().transClientAuto(loading ? "quick_screen_loading" : failed ? "quick_screen_failed" : "quick_screen_title"));
        }
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
        InputWidget search = new InputWidget(this);
        search.id("search");
        search.bounds(new ScreenCoordinate(26, 70, width - 52, 20));
        search.value(query);
        search.text(CustomQuickActionEditor.t("quick_screen_search"));
        search.onTextChanged(value -> {
            query = value;
            offset = 0;
            updateRows();
        });
        addWidget(search);
        modFilter = new DropdownSelectWidget(this);
        modFilter.id("mod_filter");
        modFilter.bounds(new ScreenCoordinate(26, 45, width - 104, 20));
        modFilter.optionEntries(modOptions());
        modFilter.selectedValues(Collections.singletonList(selectedMod));
        modFilter.onSelectionChanged(values -> {
            selectedMod = values.isEmpty() ? "" : values.get(0);
            offset = 0;
            updateRows();
        });
        addWidget(modFilter);
        ButtonWidget close = new ButtonWidget(this);
        close.id("close");
        close.bounds(new ScreenCoordinate(width - 70, 45, 44, 20));
        close.text(CustomQuickActionEditor.t("cancel").content());
        close.onClick(button -> onClose());
        addWidget(close);
        buttons.clear();
        tooltips.clear();
        int count = Math.max(1, (height - 131) / 25);
        for (int i = 0; i < count; i++) {
            ButtonWidget row = new ButtonWidget(this);
            row.id("screen_" + i);
            row.bounds(new ScreenCoordinate(26, 98 + i * 25, width - 62, 23));
            TooltipWidget tooltip = new TooltipWidget(this);
            tooltip.id("screen_tip_" + i);
            tooltip.popupAtScreenCoords(true);
            tooltip.bounds(new ScreenCoordinate(0, 0, width - 62, 23));
            row.addChild(tooltip);
            tooltips.add(tooltip);
            buttons.add(row);
            addWidget(row);
        }
        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.bounds(new ScreenCoordinate(width - 31, 98, 5, count * 25));
        scrollbar.orientation(EnumOrientation.VERTICAL).minValue(0).scrollStep(1).visibleSize(count)
                .onValueChanged(value -> {
                    offset = (int) Math.round(value);
                    updateRows();
                });
        addWidget(scrollbar);
        updateRows();
    }

    private void updateRows() {
        if (scrollbar == null) return;
        List<NavigationUiSession.Action> matches = new ArrayList<>();
        String filter = query.toLowerCase(Locale.ROOT);
        for (NavigationUiSession.Action action : actions())
            if (action.label().toVanilla().getString().toLowerCase(Locale.ROOT).contains(filter)) matches.add(action);
        offset = Math.max(0, Math.min(offset, Math.max(0, matches.size() - buttons.size())));
        scrollbar.maxValue(Math.max(0, matches.size() - buttons.size())).value(offset);
        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget button = buttons.get(i);
            int index = offset + i;
            button.visible(index < matches.size());
            if (index < matches.size()) {
                NavigationUiSession.Action action = matches.get(index);
                String full = action.label().toVanilla().getString();
                int maxWidth = Math.max(0, (int) button.bounds().width() - 12);
                button.text(QuickActionTextLayout.ellipsize(full, maxWidth, font::width));
                button.textMaxWidth(maxWidth);
                tooltips.get(i).text(QuickActionTextLayout.wrap(full, Math.max(40, Math.min(320, width - 24)), font::width));
                button.enabled(action.enabled());
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
        font.draw(graphics, CustomQuickActionEditor.t(loading ? "quick_screen_loading" : failed ? "quick_screen_failed" : "quick_screen_title").content(), 26, 27, getEffectiveTheme().textPrimary());
        super.renderWidgets(graphics, partialTicks);
    }
}
