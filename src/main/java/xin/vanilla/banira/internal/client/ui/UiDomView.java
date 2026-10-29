package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.input.BaniraPressGestureState;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import xin.vanilla.banira.common.data.Component;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongSupplier;

public class UiDomView extends NativeUiView {
    private UiDomAccess dom;
    private final UiDomView root;
    private final UiDomAccess.Node scope;
    private String prefix;
    private boolean textTools;
    private final Set<String> normalizedCloses = new HashSet<>();
    private UiGameElement closeGlyph;
    private final Map<String, UiDomAccess.Node> nodes = new LinkedHashMap<>();
    private final List<UiDomAccess.Node> ownedNodes = new ArrayList<>();
    private final Set<String> listeners = new HashSet<>();
    private final Map<String, VirtualRows> lists = new LinkedHashMap<>();
    private final Map<String, UiKeyCapture> keys = new HashMap<>();
    private Consumer<Throwable> onFailure = failure -> {
        throw new IllegalStateException("UI binding failed", failure);
    };
    private UiSelectPopup popup;
    private AutoCloseable color;
    private boolean coloring;
    private long colorToken;
    private boolean failed;
    private UiDomAccess.Node scrollContent;
    private UiDomAccess.Node detailContent;
    private double restoredDetail = Double.NaN;
    private boolean layoutRestored;
    private BooleanSupplier scrollReady = () -> true;
    private DoubleUnaryOperator scrollRequest;
    private double pendingScrollTarget = Double.NaN;
    private final LongSupplier clock;
    private final Map<String, BaniraPressGestureState> presses = new LinkedHashMap<>();
    private final Set<String> suppressClicks = new HashSet<>();
    private final Set<UiDomView> pressedViews = Collections.newSetFromMap(new IdentityHashMap<>());

    public UiDomView(Object screen, UiDomAccess dom) {
        this(screen, dom, System::currentTimeMillis);
    }

    public UiDomView(Object screen, UiDomAccess dom, LongSupplier clock) {
        super(screen);
        this.clock = Objects.requireNonNull(clock, "clock");
        root = this;
        scope = null;
        prefix = "";
        if (dom != null) attach(dom);
    }

    private UiDomView(UiDomView parent, UiDomAccess.Node scope, String prefix) {
        super(parent.screen());
        root = parent.root;
        dom = parent.dom;
        this.scope = scope;
        this.prefix = prefix;
        clock = parent.clock;
        restoreState(parent.state());
        gate(parent::live);
    }

    public void failureHandler(Consumer<Throwable> handler) {
        root.onFailure = Objects.requireNonNull(handler, "handler");
    }

    public void scrollRestoration(BooleanSupplier ready, DoubleUnaryOperator request) {
        if (scope != null || layoutRestored) throw new IllegalStateException("Scroll restoration already owned");
        scrollReady = Objects.requireNonNull(ready, "ready");
        scrollRequest = Objects.requireNonNull(request, "request");
    }

    protected final void attach(UiDomAccess access) {
        if (dom != null) throw new IllegalStateException("Document already attached");
        dom = Objects.requireNonNull(access, "access");
        if (scope == null && screen() != null) resources.own(dom.overlayRoot().on("keydown", event -> {
            if (event.keyCode() != 258 || !live()) return;
            if (modal()) {
                event.consume();
                return;
            }
            try {
                UiDomAccess.Node target = dom.nextFocus((event.modifiers() & 1) != 0);
                if (target == null) return;
                if (!target.focused()) state().selection("", 0, 0);
                target.focus();
                event.consume();
            } catch (RuntimeException | LinkageError failure) {
                fail(failure);
            }
        }));
        scrollContent = dom.find("content");
        detailContent = scope == null ? dom.find("detail") : null;
        if (detailContent != null) resources.own(detailContent.on("scroll", event -> {
            if (live() && !modal() && restoredDetail == state().detailScroll()) {
                state().detailScroll(detailContent.scrollTop());
                restoredDetail = state().detailScroll();
            }
        }));
        if (scope == null && scrollContent != null) resources.own(scrollContent.on("scroll", event -> {
            if (live() && !modal() && lists.isEmpty() && layoutRestored)
                state().scrollOffset(scrollContent.scrollTop());
        }));
        if (scope == null && scrollContent != null) {
            resources.own(scrollContent.on("wheel", event -> takeScrollOwnership()));
            resources.own(scrollContent.on("mousedown", event -> {
                if (event.button() == 0) takeScrollOwnership();
            }));
        }
        if (theme != null) changed("theme");
        for (String id : controls.keySet()) changed(id);
        for (String id : texts.keySet()) changed(id);
        for (String id : tooltips.keySet()) changed(id);
        for (String id : values.keySet()) changed(id);
        for (String id : drafts.keySet()) changed(id);
        for (String id : enabled.keySet()) changed(id);
        for (String id : gameElements.keySet()) changed(id);
        for (String id : rowProviders.keySet()) changed(id);
        for (String id : actionIds()) subscribed(id);
        for (String id : longActionIds()) subscribed(id);
        for (String id : pointerIds()) subscribed(id);
    }

    @Override
    protected boolean live() {
        return super.live() && !root.failed && (dom == null || dom.current());
    }

    private UiDomAccess.Node make(String tag, UiDomAccess.Node parent, String css) {
        UiDomAccess.Node node = dom.create(tag);
        if (css != null) node.attribute("class", css);
        parent.append(node);
        ownedNodes.add(node);
        return node;
    }

    private UiDomAccess.Node node(String id) {
        if (nodes.containsKey(id)) {
            UiDomAccess.Node existing = nodes.get(id);
            if (scope != null) existing.removeAttribute("style");
            if ((id.equals("input") || id.equals("pick") || id.equals("reset") || id.equals("delete") || id.equals("apply")) && nodes.containsKey("editor")) {
                nodes.get("editor").removeAttribute("style");
            }
            return existing;
        }
        UiDomAccess.Node node;
        if (scope == null) node = dom.find(id);
        else if (id.equals("input") || id.equals("pick") || id.equals("reset") || id.equals("delete") || id.equals("apply")) {
            UiDomAccess.Node editor = nodes.get("editor");
            if (editor == null) {
                editor = make("div", scope, "editor");
                nodes.put("editor", editor);
            } else editor.removeAttribute("style");
            node = make(id.equals("input") ? "input" : "button", editor, id.equals("input") ? "input" : "tool");
        } else node = make(id.equals("open") ? "button" : "div", scope,
                id.equals("label") ? "label" : id.equals("open") ? "row-action"
                        : id.equals("field_unit") ? "paragraph field-unit" : "paragraph");
        if (node != null) nodes.put(id, node);
        return node;
    }

    @Override
    public void tooltip(String id, Component text) {
        if (!live()) return;
        tooltips.put(id, Objects.requireNonNull(text, "text"));
        if (dom == null) return;
        UiDomAccess.Node node = node(id);
        if (node == null) return;
        String tooltip = text.toVanilla().getString();
        node.tooltip(enabled.getOrDefault(id, true) ? tooltip : "");
        if (gameElements.get(id) != null || scope != null && texts.containsKey(id)
                && toolPreset(id, texts.get(id).toVanilla().getString()) != null) node.attribute("aria-label", tooltip);
    }

    @Override
    protected void changed(String id) {
        if (dom == null || !live()) return;
        if (scope == null && id.equals("history_detail") && values.containsKey(id)) {
            UiDomAccess.Node workspace = dom.find("history_workspace");
            if (workspace != null)
                workspace.attribute("class", "notification-columns" + (values.get(id).asBoolean() ? " detail-open" : ""));
            return;
        }
        if (id.equals("theme")) {
            if (theme != null) {
                dom.theme(theme);
                dom.overlayRoot().attribute("style", UiColorBridge.tokens(theme));
            }
            return;
        }
        if (id.equals("preview")) {
            dom.hudPosition(hudX, hudY);
            return;
        }
        if (scope == null && id.equals("search_hint")) {
            UiDomAccess.Node search = node("search");
            if (search != null) search.attribute("placeholder", texts.get(id).toVanilla().getString());
            return;
        }
        if (rowProviders.containsKey(id)) {
            VirtualRows old = lists.remove(id);
            double independentPosition = old != null && !old.tracksPageScroll
                    ? old.restored ? old.scroller.scrollTop() / old.rowHeight : old.initialPosition : 0;
            if (old != null) old.close();
            UiDomAccess.Node container = node(id);
            if (container != null) lists.put(id, new VirtualRows(id, container, independentPosition));
            return;
        }
        if (scope != null && (id.equals("hint") || id.equals("tooltip") || id.equals("validation"))) {
            if (id.equals("hint")) node("input").attribute("placeholder", texts.get(id).toVanilla().getString());
            else if (id.equals("validation")) {
                String message = texts.get(id).toVanilla().getString();
                if (message.isEmpty()) node("input").removeAttribute("aria-invalid");
                else node("input").attribute("aria-invalid", "true");
                node("input").tooltip(message);
            } else node("label").tooltip(texts.get(id).toVanilla().getString());
            return;
        }
        UiDomAccess.Node node = node(id);
        if (node == null) return;
        if (gameElements.containsKey(id)) {
            node.gameElement(gameElements.get(id));
            if (scope != null && id.equals("icon")) node.attribute("class", "game-icon row-icon");
            ScreenCoordinate area = gameAreas.get(id);
            if (area != null) node.attribute("style", "position:absolute;left:" + area.x() + "px;top:" + area.y()
                    + "px;width:" + area.width() + "px;height:" + area.height() + "px;display:"
                    + (area.width() == 0 || area.height() == 0 ? "none" : "block"));
        }
        UiControlSpec control = controls.get(id);
        if (control != null) configure(id, node, control);
        if (texts.containsKey(id) && !closeButton(id)) {
            String text = texts.get(id).toVanilla().getString();
            ButtonWidget.PresetStyle preset = scope == null || textTools ? null : toolPreset(id, text);
            if (scope != null && id.equals("apply")) node.attribute("class", preset == null ? "row-action" : "tool");
            node.text(preset == null ? text : "");
            node.component(texts.get(id));
            if (preset != null) {
                node.gameElement(UiGameElement.preset(preset));
                node.attribute("aria-label", text);
            } else if (scope != null && id.equals("apply")) {
                node.gameElement(null);
                node.removeAttribute("aria-label");
            }
            if (id.equals("label") || preset != null) node.tooltip(text);
        }
        if (tooltips.containsKey(id)) {
            String tooltip = tooltips.get(id).toVanilla().getString();
            node.tooltip(tooltip);
            if (gameElements.get(id) != null || scope != null && texts.containsKey(id)
                    && toolPreset(id, texts.get(id).toVanilla().getString()) != null)
                node.attribute("aria-label", tooltip);
        }
        if (enabled.containsKey(id)) {
            if (enabled.get(id)) node.removeAttribute("disabled");
            else {
                node.attribute("disabled", "");
                node.tooltip("");
                BaniraPressGestureState press = presses.get(id);
                if (press != null) press.release();
                suppressClicks.remove(id);
                UiKeyCapture capture = keys.get(id);
                if (capture != null) capture.clear();
                if (controls.containsKey(id)) dismissOverlay();
            }
            if (scope != null && nodes.containsKey("pick")) {
                if (enabled.get(id)) nodes.get("pick").removeAttribute("disabled");
                else nodes.get("pick").attribute("disabled", "");
            }
        }
        UiValue value = values.get(id);
        if (value != null) {
            switch (value.kind()) {
                case BOOLEAN:
                    node.attribute("type", "checkbox");
                    node.checked(value.asBoolean());
                    break;
                case DECIMAL:
                    node.value(value.asDecimal().toPlainString());
                    break;
                case KEY_CHORDS:
                    node.value(String.join("+", value.asKeyChords()));
                    break;
                case STRING_LIST:
                    node.value(selectionLabel(control, value.asStrings()));
                    break;
                default:
                    node.value(control != null && control.kind() == UiControlSpec.Kind.SELECT
                            ? selectionLabel(control, Collections.singletonList(value.asText())) : value.asText());
            }
        }
        if (drafts.containsKey(id)) node.value(drafts.get(id));
        bindInput(id, node);
        restoreSelection(id, node);
        normalizeClose(id);
    }

    private boolean closeButton(String id) {
        return scope == null && (id.equals("close") || id.equals("cancel") && dom.find("cancel_close_icon") != null);
    }

    private void normalizeClose(String changed) {
        if (scope != null) return;
        String id = changed.equals("close") || changed.equals("close_icon") ? "close"
                : (changed.equals("cancel") || changed.equals("cancel_close_icon")) && dom.find("cancel_close_icon") != null ? "cancel" : null;
        if (id == null) return;
        UiDomAccess.Node button = dom.find(id);
        if (button == null) return;
        String glyphId = id.equals("close") ? "close_icon" : "cancel_close_icon";
        UiDomAccess.Node glyph = nodes.get(glyphId);
        if (glyph == null) glyph = dom.find(glyphId);
        if (normalizedCloses.add(id)) {
            button.text("");
            if (glyph == null) glyph = make("div", button, "ui-close-glyph");
            else button.append(glyph);
            nodes.put(glyphId, glyph);
        }
        button.attribute("class", "ui-close");
        button.gameElement(null);
        glyph.attribute("class", "ui-close-glyph");
        if (closeGlyph == null) closeGlyph = UiGameElement.sprite(xin.vanilla.banira.client.data.Texture.of(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("apricityui", "apricity/banira_codex/icons/close.png"), 16, 16));
        glyph.gameElement(closeGlyph);
        if (!enabled.getOrDefault(id, true)) {
            button.tooltip("");
            button.blur();
        } else {
            String label = tooltips.containsKey(id) ? tooltips.get(id).toVanilla().getString()
                    : texts.containsKey(id) ? texts.get(id).toVanilla().getString()
                    : xin.vanilla.banira.BaniraComponent.get().transClientAuto("close").toString();
            button.tooltip(label);
            button.attribute("aria-label", label);
        }
    }

    private String selectionLabel(UiControlSpec control, List<String> selected) {
        List<String> labels = new ArrayList<>();
        for (String value : selected) {
            String label = value;
            if (control != null) for (DropdownOption option : control.options()) {
                if (option.value().equals(value)) {
                    label = option.displayLabel();
                    break;
                }
            }
            labels.add(label);
        }
        return String.join(", ", labels);
    }

    private static ButtonWidget.PresetStyle toolPreset(String id, String text) {
        switch (id) {
            case "reset":
                return ButtonWidget.PresetStyle.RESET;
            case "delete":
                return "\u21b6".equals(text) ? ButtonWidget.PresetStyle.RESET : ButtonWidget.PresetStyle.CLOSE;
            case "apply":
                return "+".equals(text) ? ButtonWidget.PresetStyle.PLUS : "\u2713".equals(text) ? ButtonWidget.PresetStyle.CHECK : null;
            default:
                return null;
        }
    }

    private void configure(String id, UiDomAccess.Node node, UiControlSpec control) {
        node.attribute("type", control.kind() == UiControlSpec.Kind.PASSWORD ? "password" : "text");
        node.attribute("maxlength", Integer.toString(control.maxLength()));
        if (control.kind() == UiControlSpec.Kind.KEY_CHORD || control.kind() == UiControlSpec.Kind.SELECT) {
            if (control.kind() == UiControlSpec.Kind.KEY_CHORD || control.inputMode() == DropdownInputMode.SELECTION_ONLY
                    || control.multiple()) node.attribute("readonly", "");
            else node.removeAttribute("readonly");
        } else node.removeAttribute("readonly");
        if (scope != null && (control.kind() == UiControlSpec.Kind.FILE || control.kind() == UiControlSpec.Kind.COLOR
                || control.kind() == UiControlSpec.Kind.PICKER || control.kind() == UiControlSpec.Kind.SELECT)) {
            UiDomAccess.Node pick = node("pick");
            pick.removeAttribute("style");
            boolean select = control.kind() == UiControlSpec.Kind.SELECT;
            pick.text(select ? textTools ? "\u2304" : "" : "...");
            pick.gameElement(select && !textTools ? UiGameElement.preset(ButtonWidget.PresetStyle.ARROW_DOWN) : null);
            pick.tooltip(texts.containsKey("label") ? texts.get("label").toVanilla().getString() : "");
            listen("pick:control", pick, "click", event -> {
                if (!enabled.getOrDefault(id, true)) return;
                event.consume();
                if (control.kind() == UiControlSpec.Kind.SELECT) openSelection(id, node, control);
                else if (control.kind() == UiControlSpec.Kind.COLOR) {
                    openColor(id, node);
                } else activate("pick");
            });
        }
    }

    private void reuse(String nextPrefix) {
        for (Map.Entry<String, UiDomAccess.Node> entry : nodes.entrySet())
            captureSelection(entry.getKey(), entry.getValue());
        super.close();
        resources = new UiOwnedResources();
        presses.clear();
        suppressClicks.clear();
        root.pressedViews.remove(this);
        listeners.clear();
        keys.clear();
        prefix = nextPrefix;
        for (Map.Entry<String, UiDomAccess.Node> entry : nodes.entrySet()) {
            UiDomAccess.Node node = entry.getValue();
            node.blur();
            node.tooltip("");
            node.gameElement(null);
            node.removeAttribute("disabled");
            node.removeAttribute("aria-invalid");
            node.removeAttribute("placeholder");
            node.removeAttribute("aria-label");
            node.removeAttribute("readonly");
            node.removeAttribute("type");
            node.removeAttribute("maxlength");
            node.attribute("style", "display:none");
            if (entry.getKey().equals("input")) node.value("");
            else if (!entry.getKey().equals("editor")) node.text("");
        }
    }

    public void scrollInput(double x, double y) {
        if (scrollContent == null) return;
        try {
            ScreenCoordinate bounds = scrollContent.bounds();
            if (x >= bounds.x() && x < bounds.x() + bounds.width() && y >= bounds.y() && y < bounds.y() + bounds.height())
                takeScrollOwnership();
        } catch (RuntimeException | LinkageError failure) {
            fail(failure);
        }
    }

    private void takeScrollOwnership() {
        if (!live() || modal() || layoutRestored || !lists.isEmpty() || scrollContent == null) return;
        double position = scrollContent.scrollTop();
        if (scrollRequest != null) position = scrollRequest.applyAsDouble(position);
        else scrollContent.scrollTop(position);
        pendingScrollTarget = position;
        state().scrollOffset(position);
        layoutRestored = true;
    }

    public void afterLayout() {
        if (!live() || dom == null || scope != null) return;
        for (UiDomView pressed : new ArrayList<>(pressedViews)) pressed.updatePresses();
        if (!live()) return;
        for (VirtualRows list : lists.values()) list.afterLayout();
        if (popup != null) popup.afterLayout();
        if (detailContent != null && detailContent.clientHeight() > 0 && restoredDetail != state().detailScroll()) {
            detailContent.scrollTop(state().detailScroll());
            restoredDetail = state().detailScroll();
        }
        if (!layoutRestored && lists.isEmpty() && scrollContent != null && scrollContent.clientHeight() > 0 && scrollReady.getAsBoolean()) {
            if (Double.isNaN(pendingScrollTarget)) {
                if (scrollRequest == null) {
                    pendingScrollTarget = state().scrollOffset();
                    scrollContent.scrollTop(pendingScrollTarget);
                } else pendingScrollTarget = scrollRequest.applyAsDouble(state().scrollOffset());
            }
            if (Math.abs(scrollContent.scrollTop() - pendingScrollTarget) < .5) {
                state().scrollOffset(pendingScrollTarget);
                layoutRestored = true;
            }
        }
    }

    private void bindInput(String id, UiDomAccess.Node node) {
        UiControlSpec control = controls.get(id);
        UiValue typed = values.get(id);
        if (control == null && typed == null) return;
        listen(id + ":input", node, "input", event -> {
            if (!enabled.getOrDefault(id, true)) return;
            captureSelection(id, node);
            UiValue current = values.get(id);
            if (control != null && control.kind() == UiControlSpec.Kind.SELECT) {
                if (!control.multiple() && control.inputMode() == DropdownInputMode.EDITABLE)
                    input(id, UiValue.text(node.value()));
            } else if (control != null && control.kind() != UiControlSpec.Kind.KEY_CHORD) inputDraft(id, node.value());
            else if (current != null && current.kind() == UiValue.Kind.BOOLEAN) input(id, UiValue.bool(node.checked()));
            else if (current != null && current.kind() == UiValue.Kind.TEXT) input(id, UiValue.text(node.value()));
            captureSelection(id, node);
        });
        for (String event : Arrays.asList("select", "keyup", "mouseup", "focus")) {
            listen(id + ":" + event, node, event, ignored -> captureSelection(id, node));
        }
        // AUI 1.2.8 deletes text without dispatching an input event.
        listen(id + ":deletion", node, "keyup", event -> {
            if ((event.keyCode() != 259 && event.keyCode() != 261) || !enabled.getOrDefault(id, true)) return;
            UiValue current = values.get(id);
            String text = node.value();
            if (control != null && control.kind() == UiControlSpec.Kind.SELECT) {
                if (!control.multiple() && control.inputMode() == DropdownInputMode.EDITABLE
                        && (current == null || !Objects.equals(current.asText(), text))) input(id, UiValue.text(text));
            } else if (control != null && control.kind() != UiControlSpec.Kind.KEY_CHORD) {
                if (!Objects.equals(drafts.get(id), text)) inputDraft(id, text);
            } else if (control == null && current != null && current.kind() == UiValue.Kind.TEXT
                    && !Objects.equals(current.asText(), text)) input(id, UiValue.text(text));
        });
        if (control != null && control.kind() == UiControlSpec.Kind.KEY_CHORD) {
            UiKeyCapture capture = keys.computeIfAbsent(id, ignored -> new UiKeyCapture());
            listen(id + ":keydown", node, "keydown", event -> {
                if (!enabled.getOrDefault(id, true)) return;
                event.consume();
                if (event.keyCode() == xin.vanilla.banira.client.data.GLFWKey.GLFW_KEY_ESCAPE) {
                    capture.clear();
                    restoreKeyValue(id, node);
                    node.blur();
                    return;
                }
                List<String> completed = capture.press(event.keyCode());
                if (completed != null) {
                    input(id, UiValue.keyChords(completed));
                    changed(id);
                } else node.value(capture.preview());
            });
            listen(id + ":keyrelease", node, "keyup", event -> {
                if (!enabled.getOrDefault(id, true)) return;
                event.consume();
                List<String> completed = capture.release(event.keyCode());
                if (completed != null) {
                    input(id, UiValue.keyChords(completed));
                    changed(id);
                } else if (capture.preview().isEmpty()) restoreKeyValue(id, node);
            });
            listen(id + ":blur", node, "blur", event -> {
                capture.clear();
                restoreKeyValue(id, node);
            });
        } else if (control != null && control.kind() == UiControlSpec.Kind.SELECT) {
            listen(id + ":open-selection", node, "click", event -> {
                event.consume();
                if (enabled.getOrDefault(id, true)) openSelection(id, node, control);
            });
        }
    }

    private void restoreKeyValue(String id, UiDomAccess.Node node) {
        UiValue value = values.get(id);
        node.value(value == null ? "" : String.join("+", value.asKeyChords()));
    }

    private void openColor(String id, UiDomAccess.Node node) {
        dismissOverlay();
        root.coloring = true;
        long token = ++root.colorToken;
        AutoCloseable opened = dom.color(node, node.value(), result -> {
            if (!live() || !root.coloring || token != root.colorToken) return;
            root.coloring = false;
            root.color = null;
            try {
                if (result != null) {
                    inputDraft(id, result);
                    node.value(result);
                }
            } catch (RuntimeException | LinkageError failure) {
                fail(failure);
            }
        });
        if (root.coloring) root.color = opened;
        else release(opened);
    }

    private static void release(AutoCloseable resource) {
        try {
            if (resource != null) resource.close();
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private boolean modal() {
        return root.popup != null || root.coloring;
    }

    private void openSelection(String id, UiDomAccess.Node anchor, UiControlSpec control) {
        dismissOverlay();
        root.popup = new UiSelectPopup(dom, anchor, control, values.get(id), value -> {
            if (live()) {
                input(id, value);
                changed(id);
            }
        }, root::live, () -> root.popup = null, this::fail);
    }

    private void fail(Throwable failure) {
        if (root.failed) return;
        root.failed = true;
        try {
            dismissOverlay();
        } catch (RuntimeException | LinkageError cleanup) {
            failure.addSuppressed(cleanup);
        }
        root.onFailure.accept(failure);
    }

    public boolean dismissOverlay() {
        boolean existed = modal();
        root.coloring = false;
        root.colorToken++;
        AutoCloseable previousColor = root.color;
        root.color = null;
        release(previousColor);
        if (root.popup == null) return existed;
        UiSelectPopup previous = root.popup;
        root.popup = null;
        previous.close();
        return true;
    }

    @Override
    protected void subscribed(String id) {
        if (dom != null && live() && pointerIds().contains(id)) {
            UiDomAccess.Node target = node(id);
            if (target != null) {
                listen(id + ":down", target, "mousedown", event -> {
                    if (event.button() == 0 || event.button() == 1) {
                        if (event.button() == 0) event.consume();
                        pointer(id, new UiPointer(UiPointer.Kind.DOWN, event.x(), event.y(), event.button()));
                    }
                });
                listen(id + ":move", dom.overlayRoot(), "mousemove", event -> pointer(id, new UiPointer(UiPointer.Kind.MOVE, event.x(), event.y(), event.button())));
                listen(id + ":up", dom.overlayRoot(), "mouseup", event -> pointer(id, new UiPointer(UiPointer.Kind.UP, event.x(), event.y(), event.button())));
                listen(id + ":leave", dom.overlayRoot(), "mouseleave", event -> pointer(id, new UiPointer(UiPointer.Kind.UP, 0, 0, 0)));
            }
        }
        if (dom == null || !live() || (!actionIds().contains(id) && !longActionIds().contains(id)) || (id.equals("pick") && scope != null))
            return;
        UiDomAccess.Node node = node(id);
        if (node != null) listen(id + ":action", node, "click", event -> {
            if (event.button() != 0 && event.button() != -1) return;
            event.consume();
            if (!suppressClicks.remove(id) && !modal()) activate(id);
        });
        if (node != null && longActionIds().contains(id)) {
            BaniraPressGestureState press = presses.computeIfAbsent(id, ignored -> new BaniraPressGestureState(clock));
            listen(id + ":press", node, "mousedown", event -> {
                if (event.button() == 0 && enabled.getOrDefault(id, true)) {
                    suppressClicks.remove(id);
                    press.press(0);
                    root.pressedViews.add(this);
                }
            });
            listen(id + ":release", node, "mouseup", event -> {
                if (event.button() == 0) press.release();
            });
            listen(id + ":leave", node, "mouseleave", event -> {
                press.release();
                suppressClicks.remove(id);
            });
        }
    }

    private void updatePresses() {
        if (!live() || modal()) {
            for (BaniraPressGestureState press : presses.values()) press.release();
        }
        for (Map.Entry<String, BaniraPressGestureState> entry : new ArrayList<>(presses.entrySet())) {
            if (!enabled.getOrDefault(entry.getKey(), true)) entry.getValue().release();
            if (entry.getValue().ready(longActionDuration(entry.getKey()))) {
                entry.getValue().fire();
                suppressClicks.add(entry.getKey());
                activateLong(entry.getKey());
            }
        }
        if (presses.values().stream().noneMatch(press -> press.pressing(0))) root.pressedViews.remove(this);
    }

    private void listen(String key, UiDomAccess.Node node, String event, Consumer<UiDomAccess.Event> action) {
        if (!listeners.add(key)) return;
        resources.own(node.on(event, input -> {
            if (!live()) return;
            if (modal()) {
                input.consume();
                return;
            }
            try {
                action.accept(input);
            } catch (RuntimeException | LinkageError failure) {
                fail(failure);
            }
        }));
    }

    private void captureSelection(String id, UiDomAccess.Node node) {
        if (node.focused()) state().selection(prefix + id, node.selectionStart(), node.selectionEnd());
    }

    private void restoreSelection(String id, UiDomAccess.Node node) {
        if (state().focusedField().equals(prefix + id)) {
            int start = state().selectionStart(), end = state().selectionEnd();
            node.focus();
            node.selection(start, end);
            state().selection(prefix + id, start, end);
        }
    }

    @Override
    public ScreenCoordinate bounds(String id) {
        UiDomAccess.Node node = dom == null ? null : node(id);
        return node == null ? super.bounds(id) : node.bounds();
    }

    @Override
    public void close() {
        for (BaniraPressGestureState press : presses.values()) press.release();
        presses.clear();
        suppressClicks.clear();
        root.pressedViews.remove(this);
        if (scrollContent != null && lists.isEmpty() && layoutRestored) state().scrollOffset(scrollContent.scrollTop());
        if (detailContent != null && restoredDetail == state().detailScroll())
            state().detailScroll(detailContent.scrollTop());
        for (Map.Entry<String, UiDomAccess.Node> entry : nodes.entrySet())
            captureSelection(entry.getKey(), entry.getValue());
        if (root == this) dismissOverlay();
        try {
            for (VirtualRows list : lists.values()) list.close();
        } finally {
            try {
                super.close();
            } finally {
                for (UiDomAccess.Node node : ownedNodes) node.remove();
                ownedNodes.clear();
                nodes.clear();
                lists.clear();
            }
        }
    }

    private final class VirtualRows implements AutoCloseable {
        final String id;
        final UiDomAccess.Node container, scroller, top, bottom;
        final List<UiDomView> rows = new ArrayList<>();
        final UiRowWindow window;
        final AutoCloseable scrolling;
        int first = -1;
        boolean updating;
        boolean restored;
        boolean focusViewportRestored;
        final double initialPosition;
        final UiRowLayout presentation;
        final int rowHeight;
        final boolean tracksPageScroll;

        VirtualRows(String id, UiDomAccess.Node container, double independentPosition) {
            this.id = id;
            this.container = container;
            presentation = rowProviders.get(id) instanceof UiRowLayout ? (UiRowLayout) rowProviders.get(id) : null;
            rowHeight = presentation == null ? 48 : presentation.rowHeight();
            if (rowHeight < 12 || rowHeight > 256) throw new IllegalArgumentException("Invalid UI row height");
            tracksPageScroll = presentation == null || presentation.tracksPageScroll();
            UiDomAccess.Node content = dom.find(presentation == null ? "content" : presentation.scrollerId());
            scroller = content != null ? content : container;
            window = new UiRowWindow(rowProviders.get(id), rowLimits.get(id));
            initialPosition = tracksPageScroll ? state().rowPosition() : independentPosition;
            top = dom.create("div");
            bottom = dom.create("div");
            container.append(top);
            scrolling = scroller.on("scroll", event -> {
                if (!live() || modal()) return;
                try {
                    restored = true;
                    update();
                } catch (RuntimeException | LinkageError failure) {
                    fail(failure);
                }
            });
            update();
        }

        void afterLayout() {
            if (scroller.clientHeight() <= 0) return;
            if (!restored) {
                restored = true;
                scroller.scrollTop(initialPosition * rowHeight);
                update(true);
                return;
            }
            update();
            if (focusViewportRestored) return;
            for (UiDomView row : rows)
                for (Map.Entry<String, UiDomAccess.Node> control : row.nodes.entrySet()) {
                    UiDomAccess.Node node = control.getValue();
                    if (!state().focusedField().equals(row.prefix + control.getKey()) || !node.focused()) continue;
                    ScreenCoordinate bounds = node.bounds(), viewport = scroller.bounds();
                    if (!(bounds.height() > 0 && bounds.width() > 0 && viewport.height() > 0)) return;
                    row.restoreSelection(control.getKey(), node);
                    double delta = bounds.y() < viewport.y() || bounds.height() > viewport.height()
                            ? bounds.y() - viewport.y()
                            : Math.max(0, bounds.y() + bounds.height() - viewport.y() - viewport.height());
                    if (!Double.isFinite(delta)) return;
                    focusViewportRestored = true;
                    if (delta != 0) {
                        scroller.scrollTop(Math.max(0, scroller.scrollTop() + delta));
                        if (tracksPageScroll) {
                            state().scrollOffset(scroller.scrollTop());
                            state().rowPosition(scroller.scrollTop() / rowHeight);
                        }
                        update();
                    }
                    return;
                }
            focusViewportRestored = true;
        }

        void update() {
            update(false);
        }

        void update(boolean force) {
            if (updating) return;
            updating = true;
            try {
                int next = Math.max(0, Math.min((int) (scroller.scrollTop() / rowHeight), Math.max(0, window.total() - 1)));
                if (restored && tracksPageScroll) {
                    state().scrollOffset(scroller.scrollTop());
                    state().rowPosition(scroller.scrollTop() / rowHeight);
                }
                if (next == first && !force) return;
                first = next;
                window.moveTo(first);
                top.attribute("style", "height:" + (first * (long) rowHeight) + "px;flex-shrink:0");
                int count = Math.min(window.realizedCount(), Math.max(1, (int) Math.ceil(scroller.clientHeight() / rowHeight) + 2));
                while (rows.size() > count) {
                    UiDomView row = rows.remove(rows.size() - 1);
                    row.close();
                    row.scope.remove();
                }
                for (int index = 0; index < count; index++) {
                    UiDomView row;
                    if (index < rows.size()) {
                        row = rows.get(index);
                        row.reuse(window.key(index) + "/");
                    } else {
                        UiDomAccess.Node element = dom.create("div");
                        element.attribute("class", "row");
                        element.attribute("style", "height:" + rowHeight + "px;flex-shrink:0");
                        container.append(element);
                        row = new UiDomView(UiDomView.this, element, window.key(index) + "/");
                        rows.add(row);
                    }
                    row.textTools = presentation != null && presentation.textTools();
                    if (presentation != null)
                        row.scope.attribute("class", "row " + presentation.rowClass(first + index));
                    window.bind(index, row);
                }
                bottom.attribute("style", "height:" + Math.max(0L, (window.total() - first - count) * (long) rowHeight) + "px;flex-shrink:0");
                container.append(bottom);
                if (count > 0 && restored && tracksPageScroll) state().scrollAnchor(window.key(0));
            } finally {
                updating = false;
            }
        }

        @Override
        public void close() {
            try {
                scrolling.close();
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            } finally {
                for (UiDomView row : rows) {
                    row.close();
                    row.scope.remove();
                }
                rows.clear();
                top.remove();
                bottom.remove();
            }
        }
    }
}
