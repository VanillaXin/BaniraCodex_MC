package xin.vanilla.banira.internal.client.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiPageSpec;

import java.util.LinkedHashMap;
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
    private final Map<String, UiHudSpec> huds = new LinkedHashMap<>();
    private final Map<Object, Entry> parents = new java.util.IdentityHashMap<>();
    private Entry active;
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
        if (active != null && active.page == page) return true;
        Entry entry = new Entry(Objects.requireNonNull(page, "page"), previousScreen);
        NativeUiView view = createView(entry);
        if (view == null) return false;
        if (active != null) {
            if (active.view != null && active.view.screen() == previousScreen) parents.put(previousScreen, active);
            detach(UiDetachReason.SWITCH);
        }
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
        return screenValidator.test(entry.nativeScreen) ? new NativeUiView(entry.nativeScreen) : null;
    }

    private boolean present(Entry entry, NativeUiView view) {
        long token = ++generation;
        entry.view = view;
        view.restoreState(entry.state);
        view.gate(() -> active == entry && generation == token);
        view.onClose(() -> close(UiCloseReason.USER_CLOSE));
        try {
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

    public void tick() {
        backend.tick();
        if (active == null) { pending = null; backendRevision = backend.revision(); return; }
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
        entry.view = null;
        backend.detach(entry.page.id());
        try { entry.page.session().detach(reason); }
        finally { view.close(); }
    }

    public void close(UiCloseReason reason) {
        Entry entry = active;
        if (entry == null) return;
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

    public void disconnect() { close(UiCloseReason.DISCONNECT); }
    public UiPageSpec page() { return active == null ? null : active.page; }
    public Object previousScreen() { return active == null ? null : active.previous; }
    public NativeUiView view() { return active == null ? null : active.view; }
    public AutoCloseable registerHud(UiHudSpec hud) {
        huds.put(hud.id(), hud);
        return () -> huds.remove(hud.id(), hud);
    }

    private static final class Entry {
        final UiPageSpec page;
        final Object previous;
        final UiViewState state = new UiViewState();
        Object nativeScreen;
        NativeUiView view;
        boolean aui;
        Entry(UiPageSpec page, Object previous) { this.page = page; this.previous = previous; }
    }
}
