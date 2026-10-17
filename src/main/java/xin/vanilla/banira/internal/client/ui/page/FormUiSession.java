package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiPageSession;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.client.gui.widget.DropdownSelectWidget;
import xin.vanilla.banira.client.util.DialogUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.GLFWKeyUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;
import xin.vanilla.banira.BaniraComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FormUiSession implements UiPageSession {
    private final InputFormScreen.Args args;
    private final List<String> drafts = new ArrayList<>();
    private final Map<Integer, List<String>> selections = new LinkedHashMap<>();
    private UiView view;
    private String error = "";
    private boolean submitted;
    private boolean submitting;
    private boolean closed;
    private boolean picking;
    private long operation;
    private long version;
    private final Map<Integer, String> errors = new LinkedHashMap<>();
    private UiOwnedResources bindings = new UiOwnedResources();
    private final Map<UiView, UiOwnedResources> rowBindings = new LinkedHashMap<>();
    private final Map<UiView, Integer> boundIndices = new LinkedHashMap<>();

    public FormUiSession(InputFormScreen.Args args) {
        this.args = Objects.requireNonNull(args, "args");
        args.validate();
        for (int index = 0; index < args.getWidgets().size(); index++) {
            InputFormScreen.Widget widget = args.getWidgets().get(index);
            drafts.add(Objects.requireNonNull(widget.defaultValue(), "defaultValue"));
            if (widget.type() == InputFormScreen.WidgetType.DROPDOWN && widget.dropdownMultiSelect()) {
                List<String> defaults = widget.dropdownDefaultValues();
                selections.put(index, new ArrayList<>(defaults != null ? defaults : widget.defaultValue().isBlank()
                        ? Collections.emptyList() : Collections.singletonList(widget.defaultValue())));
            }
        }
    }

    public InputFormScreen.Args args() { return args; }
    public String error() { return error; }
    public Map<Integer, String> errors() { return java.util.Collections.unmodifiableMap(errors); }
    public long version() { return version; }
    public String draft(String name) { return drafts.get(index(name)); }

    public void draft(String name, String text) {
        draft(index(name), text);
    }

    public String draft(int index) { return drafts.get(index); }
    public void draft(int index, String text) {
        InputFormScreen.Widget widget = args.getWidgets().get(index);
        if (widget.disabled() || submitted || closed) return;
        if (selections.containsKey(index)) throw new IllegalArgumentException("Selection requires structured values");
        acceptNative(index, text);
    }

    public void acceptNative(int index, String text) {
        if (closed || submitted) return;
        if (!Objects.equals(drafts.get(index), text)) { drafts.set(index, Objects.requireNonNull(text, "text")); version++; }
    }

    public void acceptNative(int index, List<String> values) {
        if (closed || submitted) return;
        if (!selections.containsKey(index)) throw new IllegalArgumentException("Not a multiple selection");
        if (!selections.get(index).equals(values)) { selections.put(index, new ArrayList<>(values)); version++; }
    }

    public UiControlSpec control(String name) {
        return control(index(name));
    }
    public UiControlSpec control(int index) {
        InputFormScreen.Widget widget = args.getWidgets().get(index);
        UiControlSpec.Kind kind;
        switch (widget.type()) {
            case NUMERIC: kind = UiControlSpec.Kind.DECIMAL; break;
            case DROPDOWN: kind = UiControlSpec.Kind.SELECT; break;
            default: kind = UiControlSpec.Kind.valueOf(widget.type().name());
        }
        List<DropdownOption> options = new ArrayList<>();
        if (widget.type() == InputFormScreen.WidgetType.DROPDOWN) {
            if (widget.dropdownOptionEntries() != null && !widget.dropdownOptionEntries().isEmpty()) options.addAll(widget.dropdownOptionEntries());
            else if (widget.dropdownEnumClass() != null) options.addAll(DropdownSelectWidget.enumOptions(widget.dropdownEnumClass()));
            else for (String value : widget.dropdownOptions()) options.add(new DropdownOption(value));
        }
        return new UiControlSpec(kind, widget.maxLength(), widget.regex(), widget.dropdownMultiSelect(), options, widget.dropdownInputMode());
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
        else if (widget.type() == InputFormScreen.WidgetType.NUMERIC) acceptNative(index, value.asDecimal().toPlainString());
        else if (widget.type() == InputFormScreen.WidgetType.KEY_CHORD) acceptNative(index, String.join("+", value.asKeyChords()));
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
        try { return submitOnce(); }
        finally { submitting = false; }
    }

    public boolean canSubmit() {
        InputFormScreen.Results result = results();
        errors.clear();
        boolean valid = !closed && !submitted;
        for (int index = 0; index < args.getWidgets().size(); index++) {
            InputFormScreen.Widget widget = args.getWidgets().get(index);
            String text = drafts.get(index);
            if (!widget.allowEmpty() && (selections.containsKey(index) ? selections.get(index).isEmpty() : text.isEmpty())) {
                valid = false;
            }
            if (!selections.containsKey(index)) {
                if (widget.type() == InputFormScreen.WidgetType.NUMERIC && !text.isEmpty()) {
                    try { new BigDecimal(text); }
                    catch (NumberFormatException invalid) { valid = false; }
                }
            }
        }
        if (!valid) return false;
        for (int index = 0; index < args.getWidgets().size(); index++) {
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
        if (!canSubmit()) { error = errors.isEmpty() ? "" : errors.values().iterator().next(); return false; }
        InputFormScreen.Results result = results();
        if (!args.getWidgets().isEmpty()) result.curIndex(args.getWidgets().size() - 1).curName(args.getWidgets().get(args.getWidgets().size() - 1).name());
        try { args.getCallback().accept(result); }
        catch (Exception failure) { result.runningResult(failure); }
        error = result.runningResult() == null ? "" : result.runningResult();
        submitted = error.isBlank();
        return submitted;
    }

    public void pick(int index) {
        if (closed || submitted || picking || args.getWidgets().get(index).disabled()) return;
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
            if (StringUtils.isNotNullOrEmpty(widget.fileFilter())) for (String part : widget.fileFilter().split("[,;\\s]+")) {
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
        detach(UiDetachReason.RELOAD);
        this.view = Objects.requireNonNull(view, "view");
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
        bindings.own(view.onAction("cancel", () -> { if (this.view == view) view.requestClose(); }));
        bindRows(view);
        updateStatus();
    }

    private void bindRows(UiView view) {
        view.rows("fields", new UiRowProvider() {
            @Override public int size() { return args.getWidgets().size(); }
            @Override public String key(int index) { return "field:" + index; }
            @Override public void bind(UiView row, int index) {
                if (FormUiSession.this.view != view) return;
                UiOwnedResources old = rowBindings.remove(row);
                if (old != null) old.close();
                if (rowBindings.size() >= 120) {
                    UiView first = rowBindings.keySet().iterator().next();
                    rowBindings.remove(first).close(); boundIndices.remove(first);
                }
                UiOwnedResources owned = new UiOwnedResources();
                rowBindings.put(row, owned);
                boundIndices.put(row, index);
                InputFormScreen.Widget widget = args.getWidgets().get(index);
                if (widget.title() != null) row.text("label", widget.title().toComponent());
                row.control("input", control(index));
                row.enabled("input", !widget.disabled());
                if (widget.hint() != null) row.text("hint", widget.hint().toComponent());
                if (widget.tooltip() != null) row.text("tooltip", widget.tooltip().toComponent());
                if (selections.containsKey(index)) row.value("input", UiValue.strings(selections.get(index)));
                else if (widget.type() != InputFormScreen.WidgetType.KEY_CHORD
                        && widget.type() != InputFormScreen.WidgetType.DROPDOWN) {
                    row.draft("input", drafts.get(index));
                    owned.own(row.onDraft("input", text -> {
                        if (FormUiSession.this.view == view) { draft(index, text); updateStatus(); }
                    }));
                } else row.value("input", get(index));
                owned.own(row.onValue("input", value -> {
                    if (FormUiSession.this.view == view) { set(index, value); updateStatus(); }
                }));
                if (widget.type() == InputFormScreen.WidgetType.FILE || widget.type() == InputFormScreen.WidgetType.PICKER) {
                    owned.own(row.onAction("pick", () -> { if (FormUiSession.this.view == view) pick(index); }));
                }
            }
        }, 120);
    }

    private void updateStatus() {
        if (view == null) return;
        view.enabled("submit", canSubmit());
        view.text("error", BaniraComponent.get().literal(error));
        for (UiView row : rowBindings.keySet()) {
            Integer index = boundIndices.get(row);
            if (index != null) row.text("validation", BaniraComponent.get().literal(errors.getOrDefault(index, "")));
        }
    }

    @Override public void tick() {
        if (view != null && !closed && args.getInvisible() != null && Boolean.TRUE.equals(args.getInvisible().get())) view.requestClose();
    }

    @Override
    public void detach(UiDetachReason reason) {
        view = null;
        try { bindings.close(); }
        finally {
            try { for (UiOwnedResources owned : rowBindings.values()) owned.close(); }
            finally { rowBindings.clear(); boundIndices.clear(); }
        }
    }

    @Override public void close(UiCloseReason reason) {
        closed = true; operation++; picking = false; detach(UiDetachReason.SWITCH);
    }
}
