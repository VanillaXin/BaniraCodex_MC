package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiTextCompletion;
import xin.vanilla.banira.api.client.ui.UiTextCompletionProvider;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class UiCompletionPopup implements AutoCloseable {
    private final UiDomAccess dom;
    private final UiDomAccess.Node input;
    private final TextCompletionState state;
    private final int maxLength;
    private final Consumer<String> changed;
    private final UiOwnedResources resources = new UiOwnedResources();
    private UiDomAccess.Node panel;
    private UiOwnedResources rows = new UiOwnedResources();
    private final List<UiDomAccess.Node> buttons = new ArrayList<>();
    private List<UiTextCompletion> rendered;
    private int active = -1;
    private ScreenCoordinate placed;

    UiCompletionPopup(UiDomAccess dom, UiDomAccess.Node input, UiTextCompletionProvider provider,
                      int maxLength, Consumer<String> changed) {
        this(dom, input, new TextCompletionState(provider), maxLength, changed);
    }

    UiCompletionPopup(UiDomAccess dom, UiDomAccess.Node input, TextCompletionState state,
                      int maxLength, Consumer<String> changed) {
        this.dom = dom;
        this.input = input;
        this.state = state;
        this.maxLength = maxLength;
        this.changed = changed;
        resources.own(input.on("keydown", event -> {
            update();
            if (state.options().isEmpty()) return;
            int key = event.keyCode();
            if (key == 264 || key == 265) {
                event.consume();
                state.move(key == 264 ? 1 : -1);
                render();
            } else if ((key == 258 && event.modifiers() == 0) || key == 257 || key == 335) {
                event.consume();
                choose(state.selected());
            } else if (key == 256) {
                event.consume();
                dismiss();
            }
        }));
        resources.own(input.on("blur", ignored -> dismiss()));
    }

    void update() {
        if (resources.closed()) return;
        state.update(input.value(), input.selectionEnd(), input.selectionStart(), dom.current() && input.focused());
        render();
    }

    boolean dismiss() {
        boolean visible = panel != null;
        state.dismiss();
        clearPanel();
        return visible;
    }

    private void choose(int index) {
        UiTextCompletion selected = state.choose(index, input.value(), input.selectionEnd());
        if (selected != null) {
            String text = selected.apply(input.value());
            if (text.length() <= maxLength) {
                input.value(text);
                input.selection(selected.cursor(), selected.cursor());
                changed.accept(text);
            }
        }
        clearPanel();
    }

    private void render() {
        List<UiTextCompletion> options = state.options();
        if (options.isEmpty()) {
            clearPanel();
            return;
        }
        if (panel == null) {
            panel = dom.create("div");
            panel.attribute("class", "popup-panel");
            dom.overlayRoot().append(panel);
        }
        if (rendered != options) {
            clearRows();
            active = -1;
            for (int index = 0; index < options.size(); index++) {
                UiDomAccess.Node button = dom.create("button");
                button.attribute("class", "popup-option");
                button.attribute("style", "height:22px;min-height:22px;flex-shrink:0");
                button.attribute("tabindex", "-1");
                button.text(options.get(index).text());
                button.tooltip(options.get(index).tooltip());
                int selected = index;
                rows.own(button.on("mousedown", event -> {
                    if (event.button() != 0) return;
                    event.consume();
                    choose(selected);
                }));
                panel.append(button);
                buttons.add(button);
            }
            rendered = options;
        }
        if (active != state.selected()) {
            active = state.selected();
            for (int index = 0; index < buttons.size(); index++)
                buttons.get(index).attribute("aria-current", Boolean.toString(index == active));
            double top = active * 22d, offset = panel.scrollTop(), height = panel.clientHeight() - 8;
            if (top < offset) panel.scrollTop(top);
            else if (height > 0 && top + 22 > offset + height) panel.scrollTop(top + 22 - height);
        }
        ScreenCoordinate box = TextCompletionLayout.bounds(input.bounds(), dom.overlayRoot().bounds(), options.size(), 22);
        double x = box.x(), y = box.y(), width = box.width(), height = box.height();
        if (placed == null || placed.x() != x || placed.y() != y || placed.width() != width || placed.height() != height) {
            panel.attribute("style", "position:absolute;z-index:1000;display:flex;flex-direction:column;overflow-y:auto;padding:4px;left:"
                    + x + "px;top:" + y + "px;width:" + width + "px;height:" + height + "px");
            placed = new ScreenCoordinate(x, y, width, height);
        }
    }

    private void clearRows() {
        rows.close();
        rows = new UiOwnedResources();
        for (UiDomAccess.Node button : buttons) button.remove();
        buttons.clear();
    }

    private void clearPanel() {
        clearRows();
        if (panel != null) panel.remove();
        panel = null;
        rendered = null;
        placed = null;
        active = -1;
    }

    @Override
    public void close() {
        resources.close();
        state.clear();
        clearPanel();
    }
}
