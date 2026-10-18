package xin.vanilla.banira.internal.client.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.ui.*;
import java.util.*;
import java.util.function.*;

public final class AuiHudHost {
    public static final class PaintedFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public PaintedFailure(Throwable cause) { super(cause); }
    }
    private static final Logger LOGGER = LogManager.getLogger();
    private final UiBackendController backend;
    private final Function<UiHudSpec, NativeUiView> factory;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private long revision;
    private boolean connected;

    public AuiHudHost(UiBackendController backend, Function<UiHudSpec, NativeUiView> factory) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.factory = Objects.requireNonNull(factory, "factory");
        revision = backend.revision();
    }

    public AutoCloseable register(UiHudSpec spec) {
        Objects.requireNonNull(spec, "spec");
        Entry next = new Entry(spec);
        Entry previous = entries.put(spec.id(), next);
        if (previous != null) {
            try { detach(previous, UiDetachReason.SWITCH); }
            catch (RuntimeException | LinkageError failure) { fail(previous, failure); }
        }
        return () -> {
            if (entries.remove(spec.id(), next)) detach(next, UiDetachReason.SWITCH);
        };
    }

    public void tick(boolean connected) {
        this.connected = connected;
        boolean reload = revision != backend.revision();
        revision = backend.revision();
        for (Entry entry : new ArrayList<>(entries.values())) {
            try {
                if (!connected || reload || !backend.wantsAui(entry.spec.id()) || !entry.spec.binding().visible())
                    detach(entry, !connected ? UiDetachReason.DISCONNECT : reload ? UiDetachReason.RELOAD : UiDetachReason.SWITCH);
            } catch (RuntimeException | LinkageError failure) { fail(entry, failure); }
        }
    }

    public boolean render(String id, Consumer<NativeUiView> draw) {
        Entry entry = entries.get(id);
        if (!connected || entry == null) return false;
        try {
            if (!backend.wantsAui(id) || !entry.spec.binding().visible()) {
                detach(entry, UiDetachReason.SWITCH);
                return false;
            }
            if (entry.view == null) {
                NativeUiView view = Objects.requireNonNull(factory.apply(entry.spec), "HUD view");
                entry.view = view;
                view.gate(() -> connected && entries.get(id) == entry && entry.view == view && backend.wantsAui(id) && entry.spec.binding().visible());
                entry.spec.binding().bind(view);
            }
            NativeUiView view = entry.view;
            if (!current(entry, view)) { detach(entry, UiDetachReason.SWITCH); return false; }
            entry.spec.binding().update(view);
            if (!current(entry, view)) { detach(entry, UiDetachReason.SWITCH); return false; }
            draw.accept(view);
            if (!current(entry, view)) { detach(entry, UiDetachReason.SWITCH); return false; }
            backend.attach(id);
            return true;
        } catch (RuntimeException | LinkageError failure) { fail(entry, failure); return failure instanceof PaintedFailure; }
    }

    private boolean current(Entry entry, NativeUiView view) {
        return connected && entries.get(entry.spec.id()) == entry && view != null && entry.view == view && !view.closed()
                && backend.wantsAui(entry.spec.id()) && entry.spec.binding().visible();
    }

    private void fail(Entry entry, Throwable failure) {
        if (entries.get(entry.spec.id()) == entry) backend.isolate(entry.spec.id());
        try { detach(entry, UiDetachReason.SWITCH); }
        catch (RuntimeException | LinkageError cleanup) { failure.addSuppressed(cleanup); }
        LOGGER.warn("AUI HUD failed; native overlay remains active: {}", entry.spec.id(), failure);
    }

    public void renderOthers(Set<String> excluded, Consumer<NativeUiView> draw) {
        for (String id : new ArrayList<>(entries.keySet())) if (!excluded.contains(id)) render(id, draw);
    }

    private void detach(Entry entry, UiDetachReason reason) {
        NativeUiView view = entry.view;
        if (view == null) return;
        entry.view = null;
        Entry replacement = entries.get(entry.spec.id());
        if (replacement == null || replacement == entry || replacement.view == null) backend.detach(entry.spec.id());
        try { entry.spec.binding().detach(reason); }
        finally { view.close(); }
    }

    private static final class Entry {
        final UiHudSpec spec;
        NativeUiView view;
        Entry(UiHudSpec spec) { this.spec = spec; }
    }
}
