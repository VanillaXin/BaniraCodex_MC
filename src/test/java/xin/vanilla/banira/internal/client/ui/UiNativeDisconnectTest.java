package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

public class UiNativeDisconnectTest {
    private static Object tracker() throws Exception {
        return Class.forName("xin.vanilla.banira.internal.client.ui.UiNativeSessions").getConstructor().newInstance();
    }

    private static void remember(Object tracker, UiPageSource page) throws Exception {
        tracker.getClass().getMethod("remember", UiPageSource.class).invoke(tracker, page);
    }

    private static void disconnect(Object tracker) throws Exception {
        tracker.getClass().getMethod("disconnect").invoke(tracker);
    }

    private static void forget(Object tracker, UiPageSource page, boolean transferred) throws Exception {
        try {
            tracker.getClass().getMethod("forget", UiPageSource.class, boolean.class).invoke(tracker, page, transferred);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Native session ownership must wait for accepted transfer", missing);
        }
    }

    private static final class Session implements UiPageSession {
        int closes, binds;
        boolean fail;
        UiCloseReason reason;

        public void bind(UiView view) {
            binds++;
        }

        public void detach(UiDetachReason reason) {
        }

        public void close(UiCloseReason reason) {
            closes++;
            this.reason = reason;
            if (fail) throw new IllegalStateException("close failure");
        }
    }

    private static final class Page implements UiPageSource {
        final Session session;

        Page(Session session) {
            this.session = session;
        }

        public String uiSurfaceId() {
            return "banira_codex:native-test";
        }

        public UiPageSpec uiPage() {
            throw new AssertionError("Native tracking must not resolve or rebuild a page");
        }

        public UiPageSession pageSession() {
            return session;
        }
    }

    @Test
    public void disconnectClosesUnboundNativeSessionExactlyOnce() throws Exception {
        Object tracker = tracker();
        Session session = new Session();
        Page page = new Page(session);
        remember(tracker, page);
        remember(tracker, page);
        disconnect(tracker);
        disconnect(tracker);
        assertEquals(1, session.closes);
        assertEquals(0, session.binds);
        assertEquals(UiCloseReason.DISCONNECT, session.reason);
    }

    @Test
    public void aliasesCloseOnceAndSuspendedNativeParentAlsoCloses() throws Exception {
        Object tracker = tracker();
        Session parent = new Session(), child = new Session();
        Page first = new Page(parent), alias = new Page(parent), second = new Page(child);
        remember(tracker, first);
        remember(tracker, alias);
        remember(tracker, second);
        disconnect(tracker);
        assertEquals(1, parent.closes);
        assertEquals(1, child.closes);
    }

    @Test
    public void transferredSessionIsLeftToItsExistingOwner() throws Exception {
        Object tracker = tracker();
        Session session = new Session();
        Page first = new Page(session), alias = new Page(session);
        remember(tracker, first);
        remember(tracker, alias);
        forget(tracker, first, true);
        disconnect(tracker);
        assertEquals(0, session.closes);
    }

    @Test
    public void rejectedTransferRetainsNativeDisconnectOwnership() throws Exception {
        Object tracker = tracker();
        Session session = new Session();
        Page first = new Page(session), alias = new Page(session);
        remember(tracker, first);
        remember(tracker, alias);
        forget(tracker, first, false);
        disconnect(tracker);
        disconnect(tracker);
        assertEquals(1, session.closes);
        assertEquals(0, session.binds);
        assertEquals(UiCloseReason.DISCONNECT, session.reason);
    }

    @Test
    public void failingCloseDoesNotRetainOrSkipOtherSessions() throws Exception {
        Object tracker = tracker();
        Session first = new Session(), second = new Session();
        first.fail = true;
        Page parent = new Page(first), child = new Page(second);
        remember(tracker, parent);
        remember(tracker, child);
        try {
            disconnect(tracker);
            fail("Expected close failure");
        } catch (InvocationTargetException failure) {
            assertTrue(failure.getCause() instanceof IllegalStateException);
        }
        assertEquals(1, first.closes);
        assertEquals(1, second.closes);
        disconnect(tracker);
        assertEquals(1, first.closes);
        assertEquals(1, second.closes);
    }
}
