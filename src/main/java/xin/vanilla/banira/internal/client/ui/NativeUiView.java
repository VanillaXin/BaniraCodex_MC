package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.data.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class NativeUiView implements UiView, AutoCloseable {
    protected final Map<String, Component> texts = new LinkedHashMap<>();
    protected final Map<String, UiValue> values = new LinkedHashMap<>();
    protected final Map<String, Boolean> enabled = new LinkedHashMap<>();
    protected final Map<String, UiRowProvider> rowProviders = new LinkedHashMap<>();
    protected final UiOwnedResources resources = new UiOwnedResources();
    private final Map<String, List<Runnable>> actions = new LinkedHashMap<>();
    private final Map<String, List<Consumer<UiValue>>> changes = new LinkedHashMap<>();
    private Object screen;
    private UiViewState state = new UiViewState();
    private BooleanSupplier current = () -> true;
    private Runnable closeAction = () -> { };

    public NativeUiView(Object screen) { this.screen = screen; }
    public Object screen() { return screen; }
    protected void screen(Object screen) { this.screen = screen; }
    public UiViewState state() { return state; }
    void restoreState(UiViewState state) { this.state = Objects.requireNonNull(state, "state"); }
    void gate(BooleanSupplier current) { this.current = current; }
    void onClose(Runnable action) { closeAction = action; }
    public void requestClose() { if (live()) closeAction.run(); }
    protected boolean live() { return !resources.closed() && current.getAsBoolean(); }

    @Override
    public void text(String id, Component text) { texts.put(id, Objects.requireNonNull(text, "text")); changed(id); }
    @Override
    public void value(String id, UiValue value) { values.put(id, Objects.requireNonNull(value, "value")); changed(id); }
    @Override
    public void enabled(String id, boolean value) { enabled.put(id, value); changed(id); }

    @Override
    public AutoCloseable onAction(String id, Runnable action) {
        Objects.requireNonNull(action, "action");
        List<Runnable> listeners = actions.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(action);
        return resources.own(() -> listeners.remove(action));
    }

    @Override
    public AutoCloseable onValue(String id, Consumer<UiValue> action) {
        Objects.requireNonNull(action, "action");
        List<Consumer<UiValue>> listeners = changes.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(action);
        return resources.own(() -> listeners.remove(action));
    }

    public void activate(String id) {
        if (!live() || !enabled.getOrDefault(id, true)) return;
        for (Runnable action : new ArrayList<>(actions.getOrDefault(id, java.util.Collections.emptyList()))) {
            if (live()) action.run();
        }
    }

    public void input(String id, UiValue value) {
        if (!live() || !enabled.getOrDefault(id, true)) return;
        UiValue previous = values.get(id);
        if (previous != null && previous.kind() != value.kind()) throw new IllegalArgumentException("UI value type changed");
        values.put(id, value);
        for (Consumer<UiValue> listener : new ArrayList<>(changes.getOrDefault(id, java.util.Collections.emptyList()))) {
            if (live()) listener.accept(value);
        }
    }

    @Override
    public void rows(String id, UiRowProvider rows, int maxRealizedRows) {
        if (maxRealizedRows < 1 || maxRealizedRows > 120) throw new IllegalArgumentException("UI row realization limit");
        rowProviders.put(id, Objects.requireNonNull(rows, "rows"));
        changed(id);
    }

    @Override
    public ScreenCoordinate bounds(String id) { return new ScreenCoordinate(0, 0, 0, 0); }
    protected void changed(String id) { }
    protected java.util.Set<String> actionIds() { return new java.util.LinkedHashSet<>(actions.keySet()); }
    public int subscriptionCount() {
        return actions.values().stream().mapToInt(List::size).sum() + changes.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public void close() {
        try { resources.close(); }
        finally { actions.clear(); changes.clear(); texts.clear(); values.clear(); enabled.clear(); rowProviders.clear(); }
    }
}
