package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudBinding;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiView;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

public class UiHudOwnershipTest {
    static class Binding implements UiHudBinding {
        boolean visible = true;
        int binds, detaches, actions;

        public boolean visible() {
            return visible;
        }

        public void bind(UiView view) {
            binds++;
            view.onAction("test", () -> actions++);
        }

        public void detach(UiDetachReason reason) {
            detaches++;
        }
    }

    static class Host {
        final UiBackendController backend = new UiBackendController(() -> true, () -> true);
        final List<NativeUiView> views = new ArrayList<>();
        final Object host;

        Host() throws Exception {
            Class<?> type;
            try {
                type = Class.forName("xin.vanilla.banira.internal.client.ui.AuiHudHost");
            } catch (ClassNotFoundException missing) {
                throw new AssertionError("Persistent HUD host missing", missing);
            }
            Function<UiHudSpec, NativeUiView> factory = spec -> {
                NativeUiView view = new NativeUiView(null);
                views.add(view);
                return view;
            };
            host = type.getConstructor(UiBackendController.class, Function.class).newInstance(backend, factory);
        }

        Object call(String name, Class<?>[] types, Object... args) throws Exception {
            try {
                return host.getClass().getMethod(name, types).invoke(host, args);
            } catch (InvocationTargetException failure) {
                if (failure.getCause() instanceof Error) throw (Error) failure.getCause();
                throw failure;
            }
        }

        AutoCloseable register(UiHudSpec spec) throws Exception {
            return (AutoCloseable) call("register", new Class[]{UiHudSpec.class}, spec);
        }

        void tick(boolean connected) throws Exception {
            backend.tick();
            call("tick", new Class[]{boolean.class}, connected);
        }

        boolean render(Consumer<NativeUiView> draw) throws Exception {
            return (boolean) call("render", new Class[]{String.class, Consumer.class}, "test:hud", draw);
        }
    }

    static UiHudSpec spec(Binding binding) {
        return new UiHudSpec("test:hud", "banira_codex/hud/overlay.html", binding);
    }

    @Test
    public void identicalSpecReplacementHasIndependentLease() throws Exception {
        Host h = new Host();
        Binding b = new Binding();
        UiHudSpec spec = spec(b);
        AutoCloseable old = h.register(spec);
        h.tick(true);
        assertTrue(h.render(v -> {
        }));
        AutoCloseable current = h.register(spec);
        old.close();
        old.close();
        h.tick(true);
        assertTrue(h.render(v -> {
        }));
        assertEquals(2, b.binds);
        assertEquals(1, b.detaches);
        current.close();
        assertFalse(h.render(v -> fail()));
        assertTrue(h.views.stream().allMatch(NativeUiView::closed));
    }

    @Test
    public void differentSpecReplacementRejectsOldCallbacks() throws Exception {
        Host h = new Host();
        Binding a = new Binding(), b = new Binding();
        AutoCloseable old = h.register(spec(a));
        h.tick(true);
        h.render(v -> {
        });
        NativeUiView first = h.views.get(0);
        h.register(spec(b));
        old.close();
        h.tick(true);
        h.render(v -> {
        });
        first.activate("test");
        assertEquals(0, a.actions);
        h.views.get(1).activate("test");
        assertEquals(1, b.actions);
    }

    @Test
    public void hiddenSurfaceDoesNotBindOrDraw() throws Exception {
        Host h = new Host();
        Binding b = new Binding();
        b.visible = false;
        h.register(spec(b));
        h.tick(true);
        assertFalse(h.render(v -> fail()));
        assertEquals(0, b.binds);
        assertTrue(h.views.isEmpty());
        b.visible = true;
        h.tick(true);
        assertTrue(h.render(v -> {
        }));
        b.visible = false;
        h.tick(true);
        assertFalse(h.render(v -> fail()));
        assertTrue(h.views.get(0).closed());
    }

    @Test
    public void switchReloadDisconnectKeepRegistrationButReleaseViews() throws Exception {
        Host h = new Host();
        Binding b = new Binding();
        AutoCloseable lease = h.register(spec(b));
        h.tick(true);
        h.render(v -> {
        });
        h.backend.requestSwitch(false);
        h.tick(true);
        assertFalse(h.render(v -> fail()));
        assertTrue(h.views.get(0).closed());
        h.backend.requestSwitch(true);
        h.tick(true);
        assertTrue(h.render(v -> {
        }));
        h.backend.requestReload();
        h.tick(true);
        assertTrue(h.views.get(1).closed());
        assertTrue(h.render(v -> {
        }));
        h.tick(false);
        assertTrue(h.views.stream().allMatch(NativeUiView::closed));
        assertFalse(h.render(v -> fail()));
        h.tick(true);
        assertTrue(h.render(v -> {
        }));
        lease.close();
        assertTrue(h.views.stream().allMatch(NativeUiView::closed));
    }

    @Test
    public void failedRendererFallsBackOnceWithoutLeavingOwnership() throws Exception {
        Host h = new Host();
        Binding b = new Binding();
        h.register(spec(b));
        h.tick(true);
        assertFalse(h.render(v -> {
            throw new IllegalStateException("draw failure");
        }));
        assertFalse(h.backend.usesAui("test:hud"));
        assertTrue(h.views.get(0).closed());
        assertFalse(h.render(v -> fail()));
        assertEquals(1, b.detaches);
    }

    @Test
    public void reentrantReplacementCannotClaimOrDetachNewOwner() throws Exception {
        Host h = new Host();
        Binding b = new Binding();
        h.register(spec(b));
        h.tick(true);
        assertFalse(h.render(v -> {
            try {
                h.register(spec(new Binding()));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }));
        assertFalse(h.backend.usesAui("test:hud"));
        assertTrue(h.render(v -> {
        }));
    }

    @Test
    public void reentrantUpdateNeverPaintsTheDetachedView() throws Exception {
        Host h = new Host();
        Binding b = new Binding() {
            @Override
            public void update(UiView view) {
                try {
                    h.register(spec(new Binding()));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        };
        h.register(spec(b));
        h.tick(true);
        java.util.concurrent.atomic.AtomicInteger draws = new java.util.concurrent.atomic.AtomicInteger();
        assertFalse(h.render(v -> draws.incrementAndGet()));
        assertEquals(0, draws.get());
    }

    @Test
    public void detachedBindingFailureStillReturnsReplacementLease() throws Exception {
        Host h = new Host();
        Binding b = new Binding() {
            @Override
            public void detach(UiDetachReason reason) {
                throw new IllegalStateException("detach failure");
            }
        };
        h.register(spec(b));
        h.tick(true);
        h.render(v -> {
        });
        AutoCloseable replacement;
        try {
            replacement = h.register(spec(new Binding()));
        } catch (Exception failure) {
            throw new AssertionError("Replacement must retain a usable lease after old cleanup fails", failure);
        }
        assertTrue(h.views.get(0).closed());
        assertTrue(h.render(v -> {
        }));
        replacement.close();
        assertTrue(h.views.get(1).closed());
    }

    @Test
    public void renderOthersHonorsVisibilityAndExclusions() throws Exception {
        Host h = new Host();
        h.register(spec(new Binding()));
        Binding other = new Binding();
        h.register(new UiHudSpec("test:other", "banira_codex/hud/overlay.html", other));
        h.tick(true);
        java.util.concurrent.atomic.AtomicInteger draws = new java.util.concurrent.atomic.AtomicInteger();
        h.call("renderOthers", new Class[]{Set.class, Consumer.class}, Set.of("test:hud"), (Consumer<NativeUiView>) v -> draws.incrementAndGet());
        assertEquals(1, draws.get());
        assertEquals(1, other.binds);
        other.visible = false;
        h.tick(true);
        h.call("renderOthers", new Class[]{Set.class, Consumer.class}, Set.of("test:hud"), (Consumer<NativeUiView>) v -> draws.incrementAndGet());
        assertEquals(1, draws.get());
        assertEquals(1, other.detaches);
    }

    @Test
    public void hiddenBindingRejectsCallbackBeforeNextTick() throws Exception {
        Host h = new Host();
        Binding b = new Binding();
        h.register(spec(b));
        h.tick(true);
        h.render(v -> {
        });
        b.visible = false;
        h.views.get(0).activate("test");
        assertEquals(0, b.actions);
    }

    @Test
    public void aPartiallyPaintedFailureDoesNotAlsoPaintNativeInTheSameFrame() throws Exception {
        Host h = new Host();
        h.register(spec(new Binding()));
        h.tick(true);
        java.lang.reflect.Constructor<?> constructor;
        try {
            constructor = Class.forName("xin.vanilla.banira.internal.client.ui.AuiHudHost$PaintedFailure").getConstructor(Throwable.class);
        } catch (ClassNotFoundException failure) {
            throw new AssertionError("Partial painting needs a single-renderer fallback boundary", failure);
        }
        RuntimeException failure = (RuntimeException) constructor.newInstance(new IllegalStateException("late renderer failure"));
        java.util.concurrent.atomic.AtomicInteger aui = new java.util.concurrent.atomic.AtomicInteger(), nativeDraws = new java.util.concurrent.atomic.AtomicInteger();
        if (!h.render(v -> {
            aui.incrementAndGet();
            throw failure;
        })) nativeDraws.incrementAndGet();
        assertEquals(1, aui.get());
        assertEquals(0, nativeDraws.get());
        assertFalse(h.backend.usesAui("test:hud"));
        if (!h.render(v -> aui.incrementAndGet())) nativeDraws.incrementAndGet();
        assertEquals(1, aui.get());
        assertEquals(1, nativeDraws.get());
    }
}
