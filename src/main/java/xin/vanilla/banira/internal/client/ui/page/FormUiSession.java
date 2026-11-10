package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.gui.widget.DropdownSelectWidget;
import xin.vanilla.banira.client.util.DialogUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.GLFWKeyUtils;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;
import xin.vanilla.banira.internal.client.ui.UiRowPresentation;

import java.math.BigDecimal;
import java.util.*;

public final class FormUiSession implements UiPageSession {
    private final InputFormScreen.Args args;
    private final java.util.function.BooleanSupplier submissionReady;
    private final Runnable prepareSubmission;
    private final List<String> drafts = new ArrayList<>();
    private final List<Integer> visibleIndices = new ArrayList<>();
    private final BitSet conditionalVisible = new BitSet();
    private final boolean hasConditionalFields;
    private java.util.function.Predicate<InputFormScreen.Widget> filter = widget -> true;
    private final Map<Integer, List<String>> selections = new LinkedHashMap<>();
    private UiView view;
    private String error = "";
    private boolean submitted;
    private boolean submitting;
    private boolean closed;
    private boolean picking;
    private long operation;
    private long version;
    private long visibilityVersion;
    private final Map<Integer, String> errors = new LinkedHashMap<>();
    private UiOwnedResources bindings = new UiOwnedResources();
    private final Map<UiView, UiOwnedResources> rowBindings = new LinkedHashMap<>();
    private final Map<UiView, Integer> boundIndices = new LinkedHashMap<>();
    private FieldPresentation presentation;
    private boolean commonPresentation;

    public interface FieldPresentation {
        int rowHeight();

        String rowClass(int originalIndex);

        void bind(UiView row, int originalIndex, InputFormScreen.Widget widget);

        void update(UiView row, int originalIndex, InputFormScreen.Widget widget, String draft, String validation);

        default boolean checkbox(InputFormScreen.Widget widget) {
            return false;
        }
    }

    public FormUiSession(InputFormScreen.Args args) {
        this(args, () -> true, () -> {
        });
    }

    FormUiSession(InputFormScreen.Args args, java.util.function.BooleanSupplier submissionReady, Runnable prepareSubmission) {
        this.args = Objects.requireNonNull(args, "args");
        this.submissionReady = Objects.requireNonNull(submissionReady, "submissionReady");
        this.prepareSubmission = Objects.requireNonNull(prepareSubmission, "prepareSubmission");
        args.validate();
        hasConditionalFields = args.getWidgets().stream().anyMatch(widget -> widget.visibleWhen() != null);
        for (int index = 0; index < args.getWidgets().size(); index++) {
            InputFormScreen.Widget widget = args.getWidgets().get(index);
            drafts.add(Objects.requireNonNull(widget.defaultValue(), "defaultValue"));
            if (widget.type() == InputFormScreen.WidgetType.DROPDOWN && widget.dropdownMultiSelect()) {
                List<String> defaults = widget.dropdownDefaultValues();
                selections.put(index, new ArrayList<>(defaults != null ? defaults : widget.defaultValue().isBlank()
                        ? Collections.emptyList() : Collections.singletonList(widget.defaultValue())));
            }
        }
        refreshVisibility();
    }

    public InputFormScreen.Args args() {
        return args;
    }

    public void filter(java.util.function.Predicate<InputFormScreen.Widget> predicate) {
        filter = Objects.requireNonNull(predicate, "predicate");
        refreshVisibility();
    }

    public boolean hasConditionalFields() {
        return hasConditionalFields;
    }

    public boolean fieldVisible(int index) {
        return conditionalVisible.get(index);
    }

    public int visibleFieldCount() {
        return conditionalVisible.cardinality();
    }

    public long visibilityVersion() {
        return visibilityVersion;
    }

    private void refreshVisibility() {
        InputFormScreen.Results values = hasConditionalFields ? results() : null;
        List<Integer> visible = new ArrayList<>();
        conditionalVisible.clear();
        for (int index = 0; index < args.getWidgets().size(); index++)
            if (args.getWidgets().get(index).visibleWhen() == null || args.getWidgets().get(index).visibleWhen().test(values)) {
                conditionalVisible.set(index);
                if (filter.test(args.getWidgets().get(index))) visible.add(index);
            }
        if (visibleIndices.equals(visible)) return;
        visibilityVersion++;
        visibleIndices.clear();
        visibleIndices.addAll(visible);
        for (UiOwnedResources owned : rowBindings.values()) owned.close();
        rowBindings.clear();
        boundIndices.clear();
        if (view != null) bindRows(view);
    }

    public String error() {
        return error;
    }

    public Map<Integer, String> errors() {
        return java.util.Collections.unmodifiableMap(errors);
    }

    public long version() {
        return version;
    }

    public String draft(String name) {
        return drafts.get(index(name));
    }

    public void draft(String name, String text) {
        draft(index(name), text);
    }

    public String draft(int index) {
        return drafts.get(index);
    }

    public void draft(int index, String text) {
        InputFormScreen.Widget widget = args.getWidgets().get(index);
        if (widget.disabled() || submitted || closed) return;
        if (selections.containsKey(index)) throw new IllegalArgumentException("Selection requires structured values");
        acceptNative(index, text);
    }

    public void acceptNative(int index, String text) {
        if (closed || submitted) return;
        if (!Objects.equals(drafts.get(index), text)) {
            drafts.set(index, Objects.requireNonNull(text, "text"));
            version++;
            if (hasConditionalFields) refreshVisibility();
        }
    }

    public void acceptNative(int index, List<String> values) {
        if (closed || submitted) return;
        if (!selections.containsKey(index)) throw new IllegalArgumentException("Not a multiple selection");
        if (!selections.get(index).equals(values)) {
            selections.put(index, new ArrayList<>(values));
            version++;
            if (hasConditionalFields) refreshVisibility();
        }
    }

    public UiControlSpec control(String name) {
        return control(index(name));
    }

    public UiControlSpec control(int index) {
        InputFormScreen.Widget widget = args.getWidgets().get(index);
        UiControlSpec.Kind kind;
        switch (widget.type()) {
            case NUMERIC:
                kind = UiControlSpec.Kind.DECIMAL;
                break;
            case DROPDOWN:
                kind = UiControlSpec.Kind.SELECT;
                break;
            default:
                kind = UiControlSpec.Kind.valueOf(widget.type().name());
        }
        List<DropdownOption> options = new ArrayList<>();
        if (widget.type() == InputFormScreen.WidgetType.DROPDOWN) {
            if (widget.dropdownOptionEntries() != null && !widget.dropdownOptionEntries().isEmpty())
                options.addAll(widget.dropdownOptionEntries());
            else if (widget.dropdownEnumClass() != null)
                options.addAll(DropdownSelectWidget.enumOptions(widget.dropdownEnumClass()));
            else for (String value : widget.dropdownOptions()) options.add(new DropdownOption(value));
        }
        return new UiControlSpec(kind, widget.maxLength(), widget.regex(), widget.dropdownMultiSelect(), options, widget.dropdownInputMode(),
                kind == UiControlSpec.Kind.TEXT ? widget.completion() : null);
    }

    public UiValue get(String name) {
        return get(index(name));
    }

    public UiValue get(int index) {
        if (selections.containsKey(index)) return UiValue.strings(selections.get(index));
        if (args.getWidgets().get(index).type() == InputFormScreen.WidgetType.NUMERIC) {
            return UiValue.decimal(new BigDecimal(drafts.get(index)));
        }
        if (args.getWidgets().get(index).type() == InputFormScreen.WidgetType.KEY_CHORD) {
            return UiValue.keyChords(GLFWKeyUtils.parseDisplayString(drafts.get(index)));
        }
        return UiValue.text(drafts.get(index));
    }

    public void set(String name, UiValue value) {
        set(index(name), value);
    }

    public void set(int index, UiValue value) {
        InputFormScreen.Widget widget = args.getWidgets().get(index);
        if (widget.disabled() || submitted || closed) return;
        if (selections.containsKey(index)) acceptNative(index, value.asStrings());
        else if (widget.type() == InputFormScreen.WidgetType.NUMERIC)
            acceptNative(index, value.asDecimal().toPlainString());
        else if (widget.type() == InputFormScreen.WidgetType.KEY_CHORD)
            acceptNative(index, String.join("+", value.asKeyChords()));
        else acceptNative(index, value.asText());
    }

    public InputFormScreen.Results results() {
        InputFormScreen.Results result = new InputFormScreen.Results();
        for (int index = 0; index < args.getWidgets().size(); index++) {
            String name = args.getWidgets().get(index).name();
            if (selections.containsKey(index)) result.values(name, index, selections.get(index));
            else result.value(name, index, drafts.get(index));
        }
        return result;
    }

    public boolean submit() {
        if (submitted || submitting || closed) return false;
        submitting = true;
        try {
            return submitOnce();
        } finally {
            submitting = false;
        }
    }

    public boolean finished() {
        return closed || submitted;
    }

    public boolean canSubmit() {
        if (closed || submitted || !submissionReady.getAsBoolean()) return false;
        InputFormScreen.Results result = results();
        errors.clear();
        boolean valid = !closed && !submitted;
        for (int index = 0; index < args.getWidgets().size(); index++) {
            if (!fieldVisible(index)) continue;
            InputFormScreen.Widget widget = args.getWidgets().get(index);
            String text = drafts.get(index);
            if (!widget.allowEmpty() && (selections.containsKey(index) ? selections.get(index).isEmpty() : text.isEmpty())) {
                valid = false;
                errors.put(index, BaniraComponent.get().transClientAuto("config_editor_validation_failed").toString());
            }
            if (!selections.containsKey(index)) {
                if (widget.type() == InputFormScreen.WidgetType.NUMERIC && !text.isEmpty()) {
                    try {
                        new BigDecimal(text);
                    } catch (NumberFormatException invalid) {
                        valid = false;
                        errors.put(index, BaniraComponent.get().transClientAuto("config_editor_validation_failed").toString());
                    }
                }
            }
        }
        if (!valid) return false;
        for (int index = 0; index < args.getWidgets().size(); index++) {
            if (!fieldVisible(index)) continue;
            InputFormScreen.Widget widget = args.getWidgets().get(index);
            String validation = widget.validator().apply(result.curIndex(index).curName(widget.name()));
            if (validation != null && !validation.isEmpty()) {
                errors.put(index, validation);
                valid = false;
            }
        }
        return valid;
    }

    private boolean submitOnce() {
        error = "";
        prepareSubmission.run();
        if (!canSubmit()) {
            error = errors.isEmpty() ? "" : errors.values().iterator().next();
            return false;
        }
        InputFormScreen.Results result = results();
        if (!args.getWidgets().isEmpty())
            result.curIndex(args.getWidgets().size() - 1).curName(args.getWidgets().get(args.getWidgets().size() - 1).name());
        try {
            args.getCallback().accept(result);
        } catch (Exception failure) {
            result.runningResult(failure);
        }
        error = result.runningResult() == null ? "" : result.runningResult();
        submitted = error.isBlank();
        return submitted;
    }

    public void pick(int index) {
        if (closed || submitted || picking || !fieldVisible(index) || args.getWidgets().get(index).disabled()) return;
        InputFormScreen.Widget widget = args.getWidgets().get(index);
        long token = ++operation;
        picking = true;
        java.util.function.Consumer<String> callback = value -> {
            if (closed || operation != token) return;
            picking = false;
            if (value == null || value.isEmpty()) return;
            draft(index, value);
            if (view != null) bindRows(view);
            updateStatus();
        };
        if (widget.type() == InputFormScreen.WidgetType.FILE) {
            List<String> extensions = new ArrayList<>();
            if (StringUtils.isNotNullOrEmpty(widget.fileFilter()))
                for (String part : widget.fileFilter().split("[,;\\s]+")) {
                    extensions.add(part.startsWith("*") ? part : "*." + part);
                }
            DialogUtils.chooseFileString(widget.title() == null ? "" : widget.title().content(), callback, extensions.toArray(new String[0]));
        } else if (widget.type() == InputFormScreen.WidgetType.COLOR) {
            DialogUtils.chooseRgbHex(widget.title() == null ? "" : widget.title().content(), callback);
        } else if (widget.type() == InputFormScreen.WidgetType.PICKER) {
            picking = false;
            BaniraClientRuntime.setScreen(Objects.requireNonNull(widget.picker().apply(BaniraClientRuntime.currentScreen(), callback), "picker screen"));
        } else picking = false;
    }

    private int index(String name) {
        for (int index = 0; index < args.getWidgets().size(); index++) {
            if (Objects.equals(name, args.getWidgets().get(index).name())) return index;
        }
        throw new IllegalArgumentException("Unknown form field: " + name);
    }

    @Override
    public void bind(UiView view) {
        bind(view, new CommonFieldPresentation());
        commonPresentation = true;
        var title = args.getHeaderTitle() != null ? args.getHeaderTitle() : args.getTitle();
        String namespace = title == null ? null : title.toComponent().modId();
        if ((namespace == null || namespace.isEmpty()) && args.getTitle() != null)
            namespace = args.getTitle().toComponent().modId();
        if ((namespace == null || namespace.isEmpty()) && args.getParentScreen() instanceof xin.vanilla.banira.api.client.ui.UiPageSource) {
            String surface = ((xin.vanilla.banira.api.client.ui.UiPageSource) args.getParentScreen()).uiSurfaceId();
            if (surface != null && surface.indexOf(':') > 0) namespace = surface.substring(0, surface.indexOf(':'));
        }
        if (namespace == null || namespace.isEmpty()) namespace = "banira_codex";
        var brand = UiModuleHeaderIcons.resolve(namespace,
                xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().allEntriesInOrder());
        view.gameElement("brand", brand);
        view.enabled("brand", brand != null);
        view.text("module_name", new xin.vanilla.banira.common.data.ScopedComponent(namespace).transClientAuto("mod_name"));
        view.text("close", BaniraComponent.get().transClientAuto("config_editor_close"));
        bindings.own(view.onAction("close", () -> {
            if (this.view == view) view.requestClose();
        }));
        updateStatus();
    }

    private final class CommonFieldPresentation implements FieldPresentation {
        public int rowHeight() {
            return args.getWidgets().stream().anyMatch(InputFormScreen.Widget::wideInput) ? 48 : 28;
        }

        public String rowClass(int index) {
            return args.getWidgets().get(index).wideInput() ? "player-config-field wide-input-field" : "player-config-field";
        }

        public void bind(UiView row, int index, InputFormScreen.Widget widget) {
            if (widget.tooltip() != null) row.text("description", widget.tooltip().toComponent());
        }

        public void update(UiView row, int index, InputFormScreen.Widget widget, String draft, String validation) {
            row.tooltip("input", !validation.isEmpty() ? BaniraComponent.get().literal(validation)
                    : widget.tooltip() != null ? widget.tooltip().toComponent()
                    : widget.title() != null ? widget.title().toComponent() : BaniraComponent.get().empty());
        }
    }

    public void bind(UiView view, FieldPresentation presentation) {
        detach(UiDetachReason.RELOAD);
        this.view = Objects.requireNonNull(view, "view");
        this.presentation = presentation;
        bindings = new UiOwnedResources();
        if (args.getHeaderTitle() != null) view.text("title", args.getHeaderTitle().toComponent());
        else if (args.getTitle() != null) view.text("title", args.getTitle().toComponent());
        view.text("submit", BaniraComponent.get().transClientAuto("submit"));
        view.text("cancel", BaniraComponent.get().transClientAuto("cancel"));
        bindings.own(view.onAction("submit", () -> {
            if (this.view != view) return;
            if (submit()) view.requestClose();
            else updateStatus();
        }));
        bindings.own(view.onAction("cancel", () -> {
            if (this.view == view) view.requestClose();
        }));
        bindRows(view);
        updateStatus();
    }

    private void bindRows(UiView view) {
        List<Integer> indices = new ArrayList<>(visibleIndices);
        UiRowProvider provider = new UiRowProvider() {
            @Override
            public int size() {
                return indices.size();
            }

            @Override
            public String key(int index) {
                return "field:" + indices.get(index);
            }

            @Override
            public void bind(UiView row, int position) {
                if (FormUiSession.this.view != view) return;
                int index = indices.get(position);
                UiOwnedResources old = rowBindings.remove(row);
                if (old != null) old.close();
                if (rowBindings.size() >= 120) {
                    UiView first = rowBindings.keySet().iterator().next();
                    rowBindings.remove(first).close();
                    boundIndices.remove(first);
                }
                UiOwnedResources owned = new UiOwnedResources();
                rowBindings.put(row, owned);
                boundIndices.put(row, index);
                InputFormScreen.Widget widget = args.getWidgets().get(index);
                if (widget.title() != null) row.text("label", widget.title().toComponent());
                boolean checkbox = presentation != null && presentation.checkbox(widget);
                if (!checkbox) row.control("input", control(index));
                row.enabled("input", !widget.disabled() && !widget.pickerReadOnly());
                if (widget.hint() != null) row.text("hint", widget.hint().toComponent());
                if (widget.tooltip() != null) row.text("tooltip", widget.tooltip().toComponent());
                if (checkbox) row.value("input", UiValue.bool(Boolean.parseBoolean(drafts.get(index))));
                else if (selections.containsKey(index)) row.value("input", UiValue.strings(selections.get(index)));
                else if (widget.type() != InputFormScreen.WidgetType.KEY_CHORD
                        && widget.type() != InputFormScreen.WidgetType.DROPDOWN) {
                    row.draft("input", drafts.get(index));
                    owned.own(row.onDraft("input", text -> {
                        if (FormUiSession.this.view == view && !widget.pickerReadOnly()) {
                            draft(index, text);
                            updateStatus();
                        }
                    }));
                } else row.value("input", get(index));
                owned.own(row.onValue("input", value -> {
                    if (FormUiSession.this.view == view && !widget.pickerReadOnly()) {
                        if (checkbox && value.kind() == UiValue.Kind.BOOLEAN)
                            draft(index, Boolean.toString(value.asBoolean()));
                        else set(index, value);
                        updateStatus();
                    }
                }));
                if (widget.type() == InputFormScreen.WidgetType.FILE || widget.type() == InputFormScreen.WidgetType.PICKER) {
                    owned.own(row.onAction("pick", () -> {
                        if (FormUiSession.this.view == view) pick(index);
                    }));
                }
                if (presentation != null) presentation.bind(row, index, widget);
                updateRowStatus(row, index);
            }
        };
        if (presentation == null) view.rows("fields", provider, 120);
        else view.rows("fields", new UiRowPresentation() {
            public int size() {
                return provider.size();
            }

            public String key(int index) {
                return provider.key(index);
            }

            public int rowHeight() {
                return presentation.rowHeight();
            }

            public boolean textTools() {
                return true;
            }

            public String rowClass(int index) {
                return presentation.rowClass(indices.get(index));
            }

            public void bind(UiView row, int index) {
                provider.bind(row, index);
            }
        }, 120);
    }

    void updateStatus() {
        if (view == null) return;
        view.enabled("submit", canSubmit());
        view.text("error", BaniraComponent.get().literal(error));
        if (commonPresentation) view.enabled("error", !error.isEmpty());
        for (UiView row : rowBindings.keySet()) {
            Integer index = boundIndices.get(row);
            if (index != null) {
                updateRowStatus(row, index);
            }
        }
    }

    private void updateRowStatus(UiView row, int index) {
        String validation = errors.getOrDefault(index, "");
        row.text("validation", BaniraComponent.get().literal(validation));
        if (presentation != null)
            presentation.update(row, index, args.getWidgets().get(index), drafts.get(index), validation);
    }

    @Override
    public void tick() {
        if (view != null && !closed && args.getInvisible() != null && Boolean.TRUE.equals(args.getInvisible().get()))
            view.requestClose();
    }

    @Override
    public void detach(UiDetachReason reason) {
        view = null;
        presentation = null;
        commonPresentation = false;
        try {
            bindings.close();
        } finally {
            try {
                for (UiOwnedResources owned : rowBindings.values()) owned.close();
            } finally {
                rowBindings.clear();
                boundIndices.clear();
            }
        }
    }

    @Override
    public void close(UiCloseReason reason) {
        closed = true;
        operation++;
        picking = false;
        detach(UiDetachReason.SWITCH);
    }
}
