package xin.vanilla.banira.internal.client.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiPageSpec;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class UiSessionHost {
    private static final Logger LOGGER = LogManager.getLogger();
    private final UiBackendController backend;
    private final BiFunction<UiPageSpec, Object, NativeUiView> auiFactory;
    private final Consumer<Object> presenter;
    private final Predicate<Object> screenValidator;
    private AuiHudHost huds;
    private final Map<Object, Entry> parents = new java.util.IdentityHashMap<>();
    private final Map<Object, Entry> suspended = new java.util.IdentityHashMap<>();
    private Entry active;
    private Entry returning;
    private Object returningScreen;
    private UiDetachReason pending;
    private long generation;
    private long backendRevision;

    public UiSessionHost(UiBackendController backend, BiFunction<UiPageSpec, Object, NativeUiView> auiFactory,
                         Consumer<Object> presenter, Predicate<Object> screenValidator) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.auiFactory = Objects.requireNonNull(auiFactory, "auiFactory");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
        this.screenValidator = Objects.requireNonNull(screenValidator, "screenValidator");
        backendRevision = backend.revision();
    }

    public boolean open(UiPageSpec page, Object previousScreen) {
        return open(page, previousScreen, new UiViewState());
    }

    public boolean open(UiPageSpec page, Object previousScreen, UiViewState initialState) {
        if (active != null && active.page == page) return true;
        returning = null;
        returningScreen = null;
        previousScreen = retainedParent(previousScreen);
        Entry entry = new Entry(Objects.requireNonNull(page, "page"), previousScreen, Objects.requireNonNull(initialState, "initialState"));
        NativeUiView view = createView(entry);
        if (view == null) return false;
        if (active != null) {
            if (active.view != null && active.view.screen() == previousScreen) parents.put(previousScreen, active);
            detach(UiDetachReason.SWITCH);
        }
        Entry suspendedParent = suspended.remove(previousScreen);
        if (suspendedParent != null) parents.put(previousScreen, suspendedParent);
        active = entry;
        pending = null;
        return present(entry, view);
    }

    private NativeUiView createView(Entry entry) {
        if (backend.wantsAui(entry.page.id())) {
            try {
                NativeUiView candidate = Objects.requireNonNull(auiFactory.apply(entry.page, entry.previous), "AUI view");
                if (screenValidator.test(candidate.screen())) {
                    entry.aui = true;
                    return candidate;
                }
                candidate.close();
                throw new IllegalArgumentException("AUI factory returned an invalid screen");
            } catch (RuntimeException | LinkageError failure) {
                backend.isolate(entry.page.id());
                LOGGER.warn("AUI page isolated; using native interface: {}", entry.page.id(), failure);
            }
        }
        entry.aui = false;
        if (entry.nativeScreen == null) entry.nativeScreen = entry.page.nativeFactory().create(entry.previous);
        if (entry.nativeScreen instanceof UiNativePage) ((UiNativePage) entry.nativeScreen).prepareUiState(entry.state);
        return screenValidator.test(entry.nativeScreen) ? new NativeUiView(entry.nativeScreen) : null;
    }

    private boolean present(Entry entry, NativeUiView view) {
        long token = ++generation;
        entry.view = view;
        view.restoreState(entry.state);
        view.gate(() -> active == entry && generation == token);
        view.onClose(() -> close(UiCloseReason.USER_CLOSE));
        try {
            view.theme(entry.page.theme());
            entry.page.session().bind(view);
            presenter.accept(view.screen());
            if (entry.aui) backend.attach(entry.page.id());
            backendRevision = backend.revision();
            return true;
        } catch (RuntimeException | LinkageError failure) {
            boolean wasAui = entry.aui;
            detach(UiDetachReason.FALLBACK);
            if (wasAui) {
                backend.isolate(entry.page.id());
                LOGGER.warn("AUI page presentation failed; using native interface: {}", entry.page.id(), failure);
                NativeUiView fallback = createView(entry);
                if (fallback != null) return present(entry, fallback);
            }
            active = null;
            LOGGER.warn("Unable to present UI page: {}", entry.page.id(), failure);
            return false;
        }
    }

    public void switchBackend(boolean enabled) {
        backend.requestSwitch(enabled);
        pending = UiDetachReason.SWITCH;
    }

    public void resize() { if (pending == null) pending = UiDetachReason.RESIZE; }
    public void reload() { backend.requestReload(); if (pending == null) pending = UiDetachReason.RELOAD; }
    public void fallback() {
        if (active != null) { backend.isolate(active.page.id()); pending = UiDetachReason.FALLBACK; }
    }

    public void suspendScreen(NativeUiView view) {
        if (active == null || active.view != view) return;
        Entry entry = active;
        suspended.put(view.screen(), entry);
        try { detach(UiDetachReason.RELOAD); }
        finally { active = null; pending = null; }
    }

    public void resumeScreen(Object screen) {
        if (active == null) { returning = suspended.get(screen); returningScreen = screen; }
    }

    public boolean ownsScreen(Object screen) {
        return active != null && active.view != null && active.view.screen() == screen;
    }

    public void screenRemoved(Object screen) {
        if (ownsScreen(screen)) suspendScreen(active.view);
    }

    public void observeScreen(Object screen) {
        if (returning != null && returningScreen != screen) { returning = null; returningScreen = null; }
        if (active != null && !ownsScreen(screen)) suspendScreen(active.view);
        if (active == null && suspended.containsKey(screen)) resumeScreen(screen);
        else if (active == null && screen instanceof UiNativePage) {
            xin.vanilla.banira.api.client.ui.UiPageSession session = ((UiNativePage) screen).uiSession();
            if (session != null) for (Entry entry : suspended.values()) if (entry.page.session() == session) {
                returning = entry; returningScreen = screen; break;
            }
        }
    }

    public boolean isReturning(Object screen) { return returning != null && returningScreen == screen; }

    private Object retainedParent(Object screen) {
        if (!(screen instanceof UiNativePage)) return screen;
        xin.vanilla.banira.api.client.ui.UiPageSession session = ((UiNativePage) screen).uiSession();
        if (session == null) return screen;
        if (active != null && active.view != null && active.page.session() == session) return active.view.screen();
        for (Map.Entry<Object, Entry> entry : suspended.entrySet()) if (entry.getValue().page.session() == session) return entry.getKey();
        for (Map.Entry<Object, Entry> entry : parents.entrySet()) if (entry.getValue().page.session() == session) return entry.getKey();
        return screen;
    }

    public void tick() {
        backend.tick();
        if (active == null && returning != null) {
            Entry entry = returning;
            returning = null;
            returningScreen = null;
            suspended.values().removeIf(candidate -> candidate == entry);
            NativeUiView restored = createView(entry);
            if (restored != null) {
                active = entry;
                present(entry, restored);
            }
        }
        if (active == null) { pending = null; backendRevision = backend.revision(); return; }
        active.page.session().tick();
        if (active == null) return;
        if (pending == null && backendRevision != backend.revision()) {
            pending = active.aui != backend.wantsAui(active.page.id()) ? UiDetachReason.SWITCH : UiDetachReason.RELOAD;
        }
        if (pending == null) return;
        UiDetachReason reason = pending;
        pending = null;
        Entry entry = active;
        detach(reason);
        NativeUiView next = createView(entry);
        if (next == null) { active = null; return; }
        present(entry, next);
    }

    private void detach(UiDetachReason reason) {
        Entry entry = active;
        if (entry == null || entry.view == null) return;
        ++generation;
        NativeUiView view = entry.view;
        if (!entry.aui && view.screen() instanceof UiNativePage) ((UiNativePage) view.screen()).captureUiState(entry.state);
        entry.view = null;
        backend.detach(entry.page.id());
        try { entry.page.session().detach(reason); }
        finally { view.close(); }
    }

    public void close(UiCloseReason reason) {
        Entry entry = active;
        if (entry == null) return;
        if (reason != UiCloseReason.DISCONNECT && entry.page.session() instanceof UiCloseGuard
                && !((UiCloseGuard) entry.page.session()).canClose(reason)) return;
        try { detach(reason == UiCloseReason.DISCONNECT ? UiDetachReason.DISCONNECT : UiDetachReason.SWITCH); }
        finally {
            active = null;
            pending = null;
            entry.page.session().close(reason);
        }
        if (reason == UiCloseReason.DISCONNECT) {
            for (Entry parent : new java.util.ArrayList<>(parents.values())) parent.page.session().close(reason);
            parents.clear();
        } else {
            Entry parent = parents.remove(entry.previous);
            if (parent == null) presenter.accept(entry.previous);
            else {
                active = parent;
                NativeUiView restored = createView(parent);
                if (restored != null) present(parent, restored);
                else active = null;
            }
        }
    }

    public void disconnect() {
        java.util.Set<Entry> remaining = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        remaining.addAll(suspended.values());
        remaining.addAll(parents.values());
        if (active != null) remaining.remove(active);
        parents.clear();
        suspended.clear();
        returning = null;
        returningScreen = null;
        try { close(UiCloseReason.DISCONNECT); }
        finally { for (Entry entry : remaining) entry.page.session().close(UiCloseReason.DISCONNECT); }
    }
    public UiPageSpec page() { return active == null ? null : active.page; }
    public Object previousScreen() { return active == null ? null : active.previous; }
    public NativeUiView view() { return active == null ? null : active.view; }
    public AutoCloseable registerHud(UiHudSpec hud) {
        if (huds == null) throw new IllegalStateException("HUD host unavailable");
        return huds.register(hud);
    }

    public void hudHost(AuiHudHost host) { this.huds = Objects.requireNonNull(host, "host"); }

    private static final class Entry {
        final UiPageSpec page;
        final Object previous;
        final UiViewState state;
        Object nativeScreen;
        NativeUiView view;
        boolean aui;
        Entry(UiPageSpec page, Object previous, UiViewState state) { this.page = page; this.previous = previous; this.state = state; }
    }
}
