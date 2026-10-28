package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class UiSelectPopup implements AutoCloseable {
    private final UiDomAccess dom;
    private final UiDomAccess.Node anchor;
    private final UiDomAccess.Node layer, panel, search, options;
    private final UiControlSpec control;
    private final Consumer<UiValue> selection;
    private final BooleanSupplier live;
    private final Runnable closed;
    private final Consumer<Throwable> failure;
    private final UiOwnedResources resources = new UiOwnedResources();
    private final List<UiDomAccess.Node> rowNodes = new ArrayList<>();
    private final List<DropdownOption> rowOptions = new ArrayList<>();
    private final UiDomAccess.Node top, bottom;
    private boolean rendering;
    private final List<String> selected = new ArrayList<>();
    private List<DropdownOption> filtered;
    private int activeIndex = -1;
    private boolean navigating;
    private String filteredQuery;
    private int renderedFirst = -1, renderedCount = -1;

    UiSelectPopup(UiDomAccess dom, UiDomAccess.Node anchor, UiControlSpec control, UiValue value,
                  Consumer<UiValue> selection, BooleanSupplier live, Runnable closed, Consumer<Throwable> failure) {
        this.dom = dom;
        this.anchor = anchor;
        this.control = control;
        this.selection = selection;
        this.live = live;
        this.closed = closed;
        this.failure = failure;
        if (value != null) {
            if (control.multiple()) selected.addAll(value.asStrings());
            else if (!value.asText().isEmpty()) selected.add(value.asText());
        }
        layer = add("div", dom.overlayRoot(), "popup-layer");
        panel = add("div", layer, "popup-panel");
        search = add("input", panel, "popup-search");
        search.attribute("placeholder", BaniraComponent.get().transClientAuto("notification_log_filter").toVanilla().getString());
        options = add("div", panel, "popup-options");
        top = add("div", options, null);
        bottom = add("div", options, null);
        listen(layer, "click", event -> {
            event.consume();
            close();
        });
        listen(panel, "click", UiDomAccess.Event::consume);
        listen(search, "input", event -> filter());
        listen(search, "keyup", event -> {
            if ((event.keyCode() == 259 || event.keyCode() == 261)
                    && !Objects.equals(filteredQuery, search.value().toLowerCase(Locale.ROOT))) filter();
        });
        listen(search, "keydown", this::key);
        listen(options, "scroll", event -> render());
        filter();
        search.focus();
    }

    private void listen(UiDomAccess.Node node, String event, Consumer<UiDomAccess.Event> handler) {
        resources.own(node.on(event, input -> {
            if (resources.closed() || !live.getAsBoolean()) return;
            try {
                handler.accept(input);
            } catch (RuntimeException | LinkageError error) {
                try {
                    close();
                } catch (RuntimeException | LinkageError cleanup) {
                    error.addSuppressed(cleanup);
                }
                failure.accept(error);
            }
        }));
    }

    private UiDomAccess.Node add(String tag, UiDomAccess.Node parent, String css) {
        UiDomAccess.Node node = dom.create(tag);
        node.attribute("class", css);
        parent.append(node);
        return node;
    }

    private void filter() {
        String query = search.value().toLowerCase(Locale.ROOT);
        filteredQuery = query;
        filtered = new ArrayList<>();
        for (DropdownOption option : control.options()) {
            if (option.valueLowerCase().contains(query) || option.displayLabelLowerCase().contains(query))
                filtered.add(option);
        }
        activeIndex = filtered.isEmpty() ? -1 : 0;
        for (int index = 0; index < filtered.size(); index++) {
            if (selected.contains(filtered.get(index).value())) {
                activeIndex = index;
                break;
            }
        }
        navigating = false;
        ScreenCoordinate area = placement(anchor.bounds(), dom.overlayRoot().bounds(), control.options().size());
        panel.attribute("style", "left:" + area.x() + "px;top:" + area.y() + "px;width:" + area.width() + "px;height:" + area.height() + "px");
        options.scrollTop(0);
        render();
    }

    private void key(UiDomAccess.Event event) {
        int code = event.keyCode();
        if (code == 256) {
            event.consume();
            close();
            return;
        }
        if (code == 258) {
            close();
            return;
        }
        if (code == 257 || code == 335) {
            event.consume();
            if (!navigating && control.inputMode() == DropdownInputMode.EDITABLE && !search.value().isEmpty())
                choose(search.value());
            else if (activeIndex >= 0 && activeIndex < filtered.size()) choose(filtered.get(activeIndex).value());
            return;
        }
        int target = activeIndex;
        if (code == 264) target++;
        else if (code == 265) target--;
        else if (code == 266 || code == 267)
            target += (code == 266 ? -1 : 1) * Math.max(1, (int) (options.clientHeight() / 22));
        else if ((code == 268 || code == 269) && (navigating || control.inputMode() != DropdownInputMode.EDITABLE))
            target = code == 268 ? 0 : filtered.size() - 1;
        else return;
        event.consume();
        navigating = true;
        activeIndex = filtered.isEmpty() ? -1 : Math.max(0, Math.min(target, filtered.size() - 1));
        if (activeIndex >= 0) {
            double top = activeIndex * 22d, offset = options.scrollTop(), height = options.clientHeight();
            if (top < offset) options.scrollTop(top);
            else if (top + 22 > offset + height) options.scrollTop(Math.max(0, top + 22 - height));
        }
        render();
    }

    static ScreenCoordinate placement(ScreenCoordinate anchor, ScreenCoordinate viewport, int optionCount) {
        double width = Math.min(Math.max(0, viewport.width() - 16), Math.max(120, anchor.width()));
        double height = Math.min(Math.max(0, viewport.height() - 16), 40 + Math.min(8, Math.max(1, optionCount)) * 22);
        double left = viewport.x() + 8, top = viewport.y() + 8, right = viewport.x() + viewport.width() - 8;
        double bottom = viewport.y() + viewport.height() - 8;
        double below = bottom - anchor.y() - anchor.height() - 4, above = anchor.y() - 4 - top;
        double x = Math.max(left, Math.min(anchor.x(), right - width));
        boolean down = below >= height || below >= above;
        double available = Math.max(0, down ? below : above);
        if (available < 62) return new ScreenCoordinate(x, top, width, height);
        height = Math.min(height, available);
        double y = down ? anchor.y() + anchor.height() + 4 : anchor.y() - 4 - height;
        return new ScreenCoordinate(x, Math.max(top, Math.min(y, bottom - height)), width, height);
    }

    void afterLayout() {
        if (options.clientHeight() > 0) render(false);
    }

    private void render() {
        render(true);
    }

    private void render(boolean force) {
        if (rendering || resources.closed()) return;
        int first = Math.max(0, Math.min((int) (options.scrollTop() / 22), Math.max(0, filtered.size() - 1)));
        int count = Math.min(filtered.size() - first, Math.min(120, Math.max(1, (int) Math.ceil(options.clientHeight() / 22) + 2)));
        if (!force && first == renderedFirst && count == renderedCount) return;
        rendering = true;
        double offset = options.scrollTop();
        try {
            top.attribute("style", "height:" + first * 22L + "px;flex-shrink:0");
            rowOptions.clear();
            for (int index = 0; index < count; index++) {
                DropdownOption option = filtered.get(first + index);
                rowOptions.add(option);
                UiDomAccess.Node button;
                if (index < rowNodes.size()) button = rowNodes.get(index);
                else {
                    button = add("button", options, "popup-option");
                    rowNodes.add(button);
                    int slot = index;
                    listen(button, "click", event -> {
                        event.consume();
                        if (live.getAsBoolean() && !resources.closed() && slot < rowOptions.size())
                            choose(rowOptions.get(slot).value());
                    });
                    listen(button, "focus", event -> {
                        if (slot < rowOptions.size()) activeIndex = filtered.indexOf(rowOptions.get(slot));
                    });
                    listen(button, "keydown", this::key);
                }
                button.text(option.displayLabel());
                button.attribute("aria-selected", Boolean.toString(selected.contains(option.value())));
                button.attribute("aria-current", Boolean.toString(first + index == activeIndex));
                button.attribute("style", "height:22px;flex-shrink:0");
                button.icon(option);
                button.tooltip(option.tooltip() == null ? option.displayLabel() : option.tooltip().toVanilla().getString());
            }
            for (int index = count; index < rowNodes.size(); index++) {
                UiDomAccess.Node node = rowNodes.get(index);
                node.attribute("style", "display:none");
                node.icon(null);
                node.tooltip("");
            }
            bottom.attribute("style", "height:" + Math.max(0, filtered.size() - first - count) * 22L + "px;flex-shrink:0");
            options.append(bottom);
            renderedFirst = first;
            renderedCount = count;
        } finally {
            options.scrollTop(offset);
            rendering = false;
        }
    }

    private void choose(String value) {
        if (control.multiple()) {
            if (!selected.remove(value)) selected.add(value);
            selection.accept(UiValue.strings(selected));
            render();
        } else {
            selection.accept(UiValue.text(value));
            close();
        }
    }

    @Override
    public void close() {
        if (resources.closed()) return;
        try {
            resources.close();
        } finally {
            try {
                for (UiDomAccess.Node node : rowNodes) node.remove();
                rowNodes.clear();
            } finally {
                top.remove();
                bottom.remove();
                rowOptions.clear();
                search.remove();
                options.remove();
                panel.remove();
                layer.remove();
                closed.run();
                if (live.getAsBoolean()) anchor.focus();
            }
        }
    }
}
