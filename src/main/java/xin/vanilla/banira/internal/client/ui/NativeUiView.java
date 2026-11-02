package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.data.Component;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class NativeUiView implements UiView, AutoCloseable {
    protected final Map<String, Component> texts = new LinkedHashMap<>();
    protected final Map<String, Component> tooltips = new LinkedHashMap<>();
    protected final Map<String, UiValue> values = new LinkedHashMap<>();
    protected final Map<String, UiControlSpec> controls = new LinkedHashMap<>();
    protected final Map<String, String> drafts = new LinkedHashMap<>();
    protected final Map<String, xin.vanilla.banira.api.client.ui.UiGameElement> gameElements = new LinkedHashMap<>();
    protected final Map<String, ScreenCoordinate> gameAreas = new LinkedHashMap<>();
    protected final Map<String, Boolean> enabled = new LinkedHashMap<>();
    protected final Map<String, UiRowProvider> rowProviders = new LinkedHashMap<>();
    protected final Map<String, Integer> rowLimits = new LinkedHashMap<>();
    protected UiOwnedResources resources = new UiOwnedResources();
    private final Map<String, List<Runnable>> actions = new LinkedHashMap<>();
    private final Map<String, List<Runnable>> longActions = new LinkedHashMap<>();
    private final Map<String, Long> longDurations = new LinkedHashMap<>();
    private final Map<String, List<Consumer<UiValue>>> changes = new LinkedHashMap<>();
    private final Map<String, List<Consumer<String>>> draftChanges = new LinkedHashMap<>();
    private final Map<String, List<Consumer<UiPointer>>> pointers = new LinkedHashMap<>();
    protected double hudX, hudY;
    private Object screen;
    private UiViewState state = new UiViewState();
    private BooleanSupplier current = () -> true;
    private Runnable closeAction = () -> {
    };
    protected BaniraColorConfig theme;

    public NativeUiView(Object screen) {
        this.screen = screen;
    }

    public Object screen() {
        return screen;
    }

    @Override
    public boolean nativeView() {
        return screen instanceof xin.vanilla.banira.client.gui.BaniraScreen;
    }

    public boolean closed() {
        return resources.closed();
    }

    protected void screen(Object screen) {
        this.screen = screen;
    }

    public UiViewState state() {
        return state;
    }

    void restoreState(UiViewState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    void gate(BooleanSupplier current) {
        this.current = current;
    }

    void onClose(Runnable action) {
        closeAction = action;
    }

    public void requestClose() {
        if (live()) closeAction.run();
    }

    protected boolean live() {
        return !resources.closed() && current.getAsBoolean();
    }

    @Override
    public void theme(BaniraColorConfig theme) {
        if (!live()) return;
        this.theme = theme;
        changed("theme");
    }

    @Override
    public void text(String id, Component text) {
        if (!live()) return;
        texts.put(id, Objects.requireNonNull(text, "text"));
        changed(id);
    }

    @Override
    public void tooltip(String id, Component text) {
        if (!live()) return;
        tooltips.put(id, Objects.requireNonNull(text, "text"));
        changed(id);
    }

    @Override
    public void value(String id, UiValue value) {
        if (!live()) return;
        requireValue(id, value);
        values.put(id, Objects.requireNonNull(value, "value"));
        changed(id);
    }

    @Override
    public void enabled(String id, boolean value) {
        if (!live()) return;
        enabled.put(id, value);
        changed(id);
    }

    @Override
    public void control(String id, UiControlSpec control) {
        if (!live()) return;
        controls.put(id, Objects.requireNonNull(control, "control"));
        changed(id);
    }

    @Override
    public void draft(String id, String draft) {
        if (!live()) return;
        requireDraftControl(id);
        drafts.put(id, Objects.requireNonNull(draft, "draft"));
        changed(id);
    }

    private void requireDraftControl(String id) {
        UiControlSpec control = controls.get(id);
        if (control == null || control.kind() == UiControlSpec.Kind.SELECT
                || control.kind() == UiControlSpec.Kind.KEY_CHORD) {
            throw new IllegalArgumentException("Control does not accept raw text drafts");
        }
    }

    @Override
    public AutoCloseable onAction(String id, Runnable action) {
        requireOpen();
        Objects.requireNonNull(action, "action");
        List<Runnable> listeners = actions.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(action);
        AutoCloseable owned = resources.own(() -> listeners.remove(action));
        subscribed(id);
        return owned;
    }

    @Override
    public AutoCloseable onLongAction(String id, Runnable action) {
        requireOpen();
        Objects.requireNonNull(action, "action");
        List<Runnable> listeners = longActions.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(action);
        AutoCloseable owned = resources.own(() -> listeners.remove(action));
        subscribed(id);
        return owned;
    }

    @Override
    public AutoCloseable onLongAction(String id, long durationMs, Runnable action) {
        if (durationMs < 1 || durationMs > 60000) throw new IllegalArgumentException("Invalid long action duration");
        longDurations.put(id, durationMs);
        return onLongAction(id, action);
    }

    protected long longActionDuration(String id) {
        return longDurations.getOrDefault(id, 2000L);
    }

    public void activateLong(String id) {
        if (!live() || !enabled.getOrDefault(id, true)) return;
        for (Runnable action : new ArrayList<>(longActions.getOrDefault(id, java.util.Collections.emptyList()))) {
            if (live()) action.run();
        }
    }

    protected java.util.Set<String> longActionIds() {
        return new java.util.LinkedHashSet<>(longActions.keySet());
    }

    @Override
    public AutoCloseable onValue(String id, Consumer<UiValue> action) {
        requireOpen();
        Objects.requireNonNull(action, "action");
        List<Consumer<UiValue>> listeners = changes.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(action);
        AutoCloseable owned = resources.own(() -> listeners.remove(action));
        subscribed(id);
        return owned;
    }

    @Override
    public AutoCloseable onDraft(String id, Consumer<String> action) {
        requireOpen();
        requireDraftControl(id);
        Objects.requireNonNull(action, "action");
        List<Consumer<String>> listeners = draftChanges.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(action);
        AutoCloseable owned = resources.own(() -> listeners.remove(action));
        subscribed(id);
        return owned;
    }

    private void requireOpen() {
        if (resources.closed()) throw new IllegalStateException("UI resources already closed");
    }

    public void inputDraft(String id, String draft) {
        if (!live() || !enabled.getOrDefault(id, true)) return;
        requireDraftControl(id);
        drafts.put(id, Objects.requireNonNull(draft, "draft"));
        for (Consumer<String> listener : new ArrayList<>(draftChanges.getOrDefault(id, java.util.Collections.emptyList()))) {
            if (live()) listener.accept(draft);
        }
    }

    @Override
    public void gameElement(String id, xin.vanilla.banira.api.client.ui.UiGameElement element) {
        if (!live()) return;
        gameElements.put(id, element);
        changed(id);
    }

    @Override
    public void gameElement(String id, xin.vanilla.banira.api.client.ui.UiGameElement element, ScreenCoordinate area) {
        if (!live()) return;
        Objects.requireNonNull(area, "area");
        if (!Double.isFinite(area.x()) || !Double.isFinite(area.y()) || !Double.isFinite(area.width())
                || !Double.isFinite(area.height()) || area.width() < 0 || area.height() < 0)
            throw new IllegalArgumentException("Invalid game element bounds");
        gameAreas.put(id, new ScreenCoordinate(area.x(), area.y(), area.width(), area.height()));
        gameElements.put(id, element);
        changed(id);
    }

    @Override
    public void hudPosition(double x, double y) {
        if (!live() || !Double.isFinite(x) || !Double.isFinite(y)) return;
        hudX = Math.max(0, Math.min(1, x));
        hudY = Math.max(0, Math.min(1, y));
        changed("preview");
    }

    @Override
    public AutoCloseable onPointer(String id, Consumer<UiPointer> action) {
        requireOpen();
        List<Consumer<UiPointer>> listeners = pointers.computeIfAbsent(id, key -> new ArrayList<>());
        listeners.add(Objects.requireNonNull(action));
        AutoCloseable owned = resources.own(() -> listeners.remove(action));
        subscribed(id);
        return owned;
    }

    public void pointer(String id, UiPointer event) {
        if (!live() || !event.finite() || !enabled.getOrDefault(id, true)) return;
        for (Consumer<UiPointer> listener : new ArrayList<>(pointers.getOrDefault(id, Collections.emptyList())))
            if (live()) listener.accept(event);
    }

    protected Set<String> pointerIds() {
        return new LinkedHashSet<>(pointers.keySet());
    }

    public void activate(String id) {
        if (!live() || !enabled.getOrDefault(id, true)) return;
        for (Runnable action : new ArrayList<>(actions.getOrDefault(id, java.util.Collections.emptyList()))) {
            if (live()) action.run();
        }
    }

    public void input(String id, UiValue value) {
        if (!live() || !enabled.getOrDefault(id, true)) return;
        requireValue(id, value);
        UiValue previous = values.get(id);
        if (previous != null && previous.kind() != value.kind())
            throw new IllegalArgumentException("UI value type changed");
        values.put(id, value);
        for (Consumer<UiValue> listener : new ArrayList<>(changes.getOrDefault(id, java.util.Collections.emptyList()))) {
            if (live()) listener.accept(value);
        }
    }

    private void requireValue(String id, UiValue value) {
        Objects.requireNonNull(value, "value");
        UiControlSpec control = controls.get(id);
        if (control == null) return;
        UiValue.Kind expected;
        switch (control.kind()) {
            case DECIMAL:
                expected = UiValue.Kind.DECIMAL;
                break;
            case KEY_CHORD:
                expected = UiValue.Kind.KEY_CHORDS;
                break;
            case SELECT:
                expected = control.multiple() ? UiValue.Kind.STRING_LIST : UiValue.Kind.TEXT;
                break;
            default:
                expected = UiValue.Kind.TEXT;
        }
        if (value.kind() != expected) throw new IllegalArgumentException("UI control value type mismatch");
    }

    @Override
    public void rows(String id, UiRowProvider rows, int maxRealizedRows) {
        if (!live()) return;
        if (maxRealizedRows < 1 || maxRealizedRows > 120)
            throw new IllegalArgumentException("UI row realization limit");
        rowProviders.put(id, Objects.requireNonNull(rows, "rows"));
        rowLimits.put(id, maxRealizedRows);
        changed(id);
    }

    @Override
    public ScreenCoordinate bounds(String id) {
        return new ScreenCoordinate(0, 0, 0, 0);
    }

    protected void changed(String id) {
    }

    protected void subscribed(String id) {
    }

    protected java.util.Set<String> actionIds() {
        return new java.util.LinkedHashSet<>(actions.keySet());
    }

    public int subscriptionCount() {
        return actions.values().stream().mapToInt(List::size).sum() + changes.values().stream().mapToInt(List::size).sum()
                + draftChanges.values().stream().mapToInt(List::size).sum() + longActions.values().stream().mapToInt(List::size).sum()
                + pointers.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public void close() {
        try {
            resources.close();
        } finally {
            actions.clear();
            longActions.clear();
            longDurations.clear();
            changes.clear();
            draftChanges.clear();
            pointers.clear();
            texts.clear();
            tooltips.clear();
            values.clear();
            controls.clear();
            drafts.clear();
            enabled.clear();
            rowProviders.clear();
            rowLimits.clear();
            gameElements.clear();
            gameAreas.clear();
        }
    }
}
