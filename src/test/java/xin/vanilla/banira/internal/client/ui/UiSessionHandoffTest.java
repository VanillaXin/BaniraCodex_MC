package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.BaniraQuickActionScreenFactory;
import xin.vanilla.banira.internal.compat.aui.AuiCapabilityContractTest;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class UiSessionHandoffTest {
    static Class<?> type(String suffix) {
        return AuiCapabilityContractTest.required("xin.vanilla.banira." + suffix);
    }

    static Object call(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
        return target.getClass().getMethod(name, types).invoke(target, arguments);
    }

    static Object call(Object target, String name) throws Exception {
        return call(target, name, new Class<?>[0]);
    }

    static Object view(Object screen) {
        try {
            return type("internal.client.ui.NativeUiView").getConstructor(Object.class).newInstance(screen);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    static final class FakeScreen {
    }

    static final class Fixture {
        final UiBackendController backend;
        final Object host;
        final Object page;
        final Object parent = new FakeScreen();
        final List<Object> shown = new ArrayList<>();
        final List<Object> bound = new ArrayList<>();
        final List<String> detached = new ArrayList<>();
        final AtomicInteger submitted = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        final List<String> parentReturnOrder = new ArrayList<>();
        final List<Object> returnedParents = new ArrayList<>();
        final String capturedParentDraft = "edited parent draft";
        String parentDraft = capturedParentDraft;
        Object currentScreen;
        String draft = "initial";

        Fixture(boolean aui, BiFunction<Object, Object, Object> factory,
                BaniraQuickActionScreenFactory nativeFactory) throws Exception {
            this(aui, factory, nativeFactory, false);
        }

        Fixture(boolean aui, BiFunction<Object, Object, Object> factory,
                BaniraQuickActionScreenFactory nativeFactory, boolean observeParentReturn) throws Exception {
            backend = new UiBackendController(() -> aui, () -> true);
            host = type("internal.client.ui.UiSessionHost")
                    .getConstructor(UiBackendController.class, BiFunction.class, Consumer.class, Predicate.class)
                    .newInstance(backend, factory, (Consumer<Object>) screen -> {
                                shown.add(screen);
                                currentScreen = screen;
                                if (screen == parent) {
                                    parentReturnOrder.add("present-parent");
                                    parentDraft = "reinitialized parent draft";
                                }
                            },
                            (Predicate<Object>) value -> value instanceof FakeScreen);
            Class<?> sessionType = type("api.client.ui.UiPageSession");
            Class<?>[] interfaces = new Class<?>[]{sessionType};
            if (observeParentReturn) {
                Class<?> parentReturnType = type("internal.client.ui.UiParentReturn");
                assertTrue(parentReturnType.isInterface());
                parentReturnType.getMethod("afterParentReturn", Object.class);
                interfaces = new Class<?>[]{sessionType, parentReturnType};
            }
            Object session = Proxy.newProxyInstance(sessionType.getClassLoader(), interfaces,
                    (proxy, method, args) -> {
                        if (method.getName().equals("bind")) {
                            Object active = args[0];
                            bound.add(active);
                            Object value = type("api.client.ui.UiValue").getMethod("text", String.class)
                                    .invoke(null, draft);
                            call(active, "value", new Class<?>[]{String.class, type("api.client.ui.UiValue")}, "draft", value);
                            call(active, "onAction", new Class<?>[]{String.class, Runnable.class},
                                    "submit", (Runnable) submitted::incrementAndGet);
                        } else if (method.getName().equals("detach")) {
                            detached.add(args[0].toString());
                        } else if (method.getName().equals("close")) {
                            closed.incrementAndGet();
                        } else if (method.getName().equals("afterParentReturn")) {
                            assertSame("Restore must receive the original parent", parent, args[0]);
                            assertSame("Presenter must finish before draft restoration", parent, currentScreen);
                            assertSame(parent, shown.get(shown.size() - 1));
                            parentReturnOrder.add("restore-parent");
                            returnedParents.add(args[0]);
                            parentDraft = capturedParentDraft;
                        }
                        return null;
                    });
            page = type("api.client.ui.UiPageSpec")
                    .getConstructor(String.class, String.class, sessionType, BaniraQuickActionScreenFactory.class)
                    .newInstance("banira_codex:test", "banira_codex/ui/test.html", session, nativeFactory);
        }

        Fixture() throws Exception {
            this(true, (page, parent) -> view(new FakeScreen()), parent -> new FakeScreen());
        }

        Fixture(boolean aui, boolean observeParentReturn) throws Exception {
            this(aui, (page, parent) -> view(new FakeScreen()), parent -> new FakeScreen(), observeParentReturn);
        }

        boolean open() throws Exception {
            return (boolean) call(host, "open", new Class<?>[]{type("api.client.ui.UiPageSpec"), Object.class}, page, parent);
        }
    }

    @Test
    public void switchReplyAndResizeCoalesceWithoutClosingSession() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        Object previousView = call(f.host, "view");
        Object state = call(previousView, "state");
        call(state, "scrollAnchor", new Class<?>[]{String.class}, "row:42");
        call(state, "focusedField", new Class<?>[]{String.class}, "search");
        call(f.host, "switchBackend", new Class<?>[]{boolean.class}, false);
        f.draft = "server reply with local draft retained";
        call(f.host, "resize");
        assertEquals(1, f.bound.size());
        call(f.host, "tick");
        assertEquals(2, f.bound.size());
        assertEquals(1, f.detached.size());
        assertEquals("SWITCH", f.detached.get(0));
        assertSame(f.page, call(f.host, "page"));
        assertSame(f.parent, call(f.host, "previousScreen"));
        Object next = call(f.host, "view");
        assertSame(state, call(next, "state"));
        assertEquals("row:42", call(state, "scrollAnchor"));
        assertEquals("search", call(state, "focusedField"));
        call(previousView, "activate", new Class<?>[]{String.class}, "submit");
        assertEquals(0, f.submitted.get());
        assertEquals(0, f.closed.get());
        call(next, "activate", new Class<?>[]{String.class}, "submit");
        assertEquals(1, f.submitted.get());
    }

    @Test
    public void hundredSwitchesReleaseEveryOldSubscription() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        for (int i = 0; i < 100; i++) {
            Object previous = call(f.host, "view");
            call(f.host, "switchBackend", new Class<?>[]{boolean.class}, i % 2 != 0);
            call(f.host, "tick");
            assertEquals(0, call(previous, "subscriptionCount"));
            assertEquals(1, call(call(f.host, "view"), "subscriptionCount"));
        }
        assertEquals(0, f.closed.get());
    }

    @Test
    public void disconnectRejectsOldEventsAndClosesOnlyOnce() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        Object old = call(f.host, "view");
        call(f.host, "disconnect");
        call(f.host, "disconnect");
        call(old, "activate", new Class<?>[]{String.class}, "submit");
        assertEquals(0, f.submitted.get());
        assertEquals(1, f.closed.get());
        assertNull(call(f.host, "page"));
    }

    @Test
    public void ownedResourcesAreReleasedOnceInReverseOrder() throws Exception {
        Object resources = type("internal.client.ui.UiOwnedResources").getConstructor().newInstance();
        List<Integer> released = new ArrayList<>();
        call(resources, "own", new Class<?>[]{AutoCloseable.class}, (AutoCloseable) () -> released.add(1));
        call(resources, "own", new Class<?>[]{AutoCloseable.class}, (AutoCloseable) () -> released.add(2));
        call(resources, "close");
        call(resources, "close");
        assertEquals(java.util.Arrays.asList(2, 1), released);
    }

    @Test
    public void childCloseResumesParentWithoutClosingItsSession() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        Object parentView = call(f.host, "view");
        Object parentState = call(parentView, "state");
        call(parentState, "scrollAnchor", new Class<?>[]{String.class}, "row:parent");
        Class<?> sessionType = type("api.client.ui.UiPageSession");
        AtomicInteger childClosed = new AtomicInteger();
        Object childSession = Proxy.newProxyInstance(sessionType.getClassLoader(), new Class<?>[]{sessionType},
                (proxy, method, args) -> { if (method.getName().equals("close")) childClosed.incrementAndGet(); return null; });
        Object child = type("api.client.ui.UiPageSpec")
                .getConstructor(String.class, String.class, sessionType, BaniraQuickActionScreenFactory.class)
                .newInstance("banira_codex:child", "banira_codex/ui/child.html", childSession,
                        (BaniraQuickActionScreenFactory) parent -> new FakeScreen());
        assertTrue((boolean) call(f.host, "open", new Class<?>[]{type("api.client.ui.UiPageSpec"), Object.class},
                child, call(parentView, "screen")));
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object reason = Enum.valueOf((Class) type("api.client.ui.UiCloseReason"), "USER_CLOSE");
        call(f.host, "close", new Class<?>[]{type("api.client.ui.UiCloseReason")}, reason);
        assertSame("Parent page must resume", f.page, call(f.host, "page"));
        assertSame(parentState, call(call(f.host, "view"), "state"));
        assertEquals("row:parent", call(parentState, "scrollAnchor"));
        assertEquals(1, childClosed.get());
        assertEquals(0, f.closed.get());
    }

    @Test
    public void reopeningSamePageDoesNotDuplicateSessionOrDisconnectClose() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.open());
        Object current = call(f.host, "view");
        assertTrue((boolean) call(f.host, "open", new Class<?>[]{type("api.client.ui.UiPageSpec"), Object.class},
                f.page, call(current, "screen")));
        assertEquals("Same page must keep its active binding", 1, f.bound.size());
        call(f.host, "disconnect");
        assertEquals(1, f.closed.get());
    }

    @Test
    public void parentDraftRestoresAfterPresenterAndOnlyOnceForNativeAndAui() throws Exception {
        for (boolean aui : new boolean[]{false, true}) {
            Fixture f = new Fixture(aui, true);
            assertTrue(f.open());
            assertNotSame(f.parent, f.currentScreen);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object reason = Enum.valueOf((Class) type("api.client.ui.UiCloseReason"), "USER_CLOSE");
            call(f.host, "close", new Class<?>[]{type("api.client.ui.UiCloseReason")}, reason);
            assertEquals(java.util.Arrays.asList("present-parent", "restore-parent"), f.parentReturnOrder);
            assertEquals(f.capturedParentDraft, f.parentDraft);
            assertSame(f.parent, f.returnedParents.get(0));
            call(f.host, "close", new Class<?>[]{type("api.client.ui.UiCloseReason")}, reason);
            call(f.host, "tick");
            call(f.host, "disconnect");
            assertEquals("Repeated close/tick/disconnect must not restore again", 1, f.returnedParents.size());
            assertEquals(2, f.parentReturnOrder.size());
            assertEquals(1, f.closed.get());
        }
    }

    @Test
    public void backendSwitchDoesNotInvokeParentReturn() throws Exception {
        Fixture f = new Fixture(true, true);
        assertTrue(f.open());
        for (boolean aui : new boolean[]{false, true}) {
            call(f.host, "switchBackend", new Class<?>[]{boolean.class}, aui);
            call(f.host, "tick");
            assertSame(f.page, call(f.host, "page"));
            assertNotSame(f.parent, f.currentScreen);
            assertTrue(f.returnedParents.isEmpty());
            assertTrue(f.parentReturnOrder.isEmpty());
            assertEquals(f.capturedParentDraft, f.parentDraft);
            assertEquals(0, f.closed.get());
        }
    }

    @Test
    public void disconnectDoesNotInvokeParentReturnForNativeOrAui() throws Exception {
        for (boolean aui : new boolean[]{false, true}) {
            Fixture f = new Fixture(aui, true);
            assertTrue(f.open());
            call(f.host, "disconnect");
            call(f.host, "disconnect");
            call(f.host, "tick");
            assertTrue(f.returnedParents.isEmpty());
            assertTrue(f.parentReturnOrder.isEmpty());
            assertEquals(f.capturedParentDraft, f.parentDraft);
            assertEquals(1, f.closed.get());
            assertNull(call(f.host, "page"));
        }
    }
}
