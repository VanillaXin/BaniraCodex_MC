package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiNativeAliasTest {
    private static final class Session implements UiPageSession {
        final AtomicInteger binds = new AtomicInteger(), closes = new AtomicInteger();
        public void bind(UiView view) { binds.incrementAndGet(); }
        public void detach(UiDetachReason reason) { }
        public void close(UiCloseReason reason) { closes.incrementAndGet(); }
    }
    public static final class Alias implements UiNativePage {
        private final Session session;
        int specQueries;
        Alias(Session session) { this.session = session; }
        public String uiSurfaceId() { return "banira_codex:alias"; }
        public UiPageSpec uiPage() { specQueries++; throw new AssertionError("Resolving a native alias must not capture stale native drafts"); }
        public UiPageSession uiSession() { return session; }
    }
    static final class Fixture {
        final Session session = new Session();
        final Alias alias = new Alias(session);
        final UiPageSpec page = new UiPageSpec("banira_codex:alias", "banira_codex/ui/form.html", session, previous -> alias);
        final UiSessionHost host = new UiSessionHost(new UiBackendController(() -> true, () -> true),
                (spec, previous) -> new NativeUiView(new Object()), screen -> { }, screen -> true);
        Fixture() { assertTrue(host.open(page, null)); host.view().state().rowPosition(42); }
    }
    @Test public void childOpenedWithNativeParentAliasRestoresOriginalAuiSession() {
        Fixture f = new Fixture();
        f.host.suspendScreen(f.host.view());
        Session child = new Session();
        f.host.open(new UiPageSpec("banira_codex:child", "banira_codex/ui/form.html", child, previous -> new Object()), f.alias);
        f.host.close(UiCloseReason.USER_CLOSE);
        assertSame("A native constructor parent must resolve to the retained AUI parent", f.page, f.host.page());
        assertEquals(42, f.host.view().state().rowPosition(), 0);
        assertEquals(2, f.session.binds.get()); assertEquals(0, f.session.closes.get()); assertEquals(0, f.alias.specQueries);
    }
    @Test public void nativeOnlyChildReturnIsDeferredWithoutRecapturingNativeDrafts() throws Exception {
        Fixture f = new Fixture();
        f.host.suspendScreen(f.host.view());
        f.host.observeScreen(f.alias);
        assertTrue("Shared router needs a pending alias-return guard before uiPage()", Arrays.stream(UiSessionHost.class.getMethods())
                .anyMatch(method -> method.getName().equals("isReturning")));
        assertEquals(true, call(f.host, "isReturning", new Class<?>[]{Object.class}, f.alias));
        f.host.tick();
        assertSame(f.page, f.host.page()); assertEquals(42, f.host.view().state().rowPosition(), 0);
        assertEquals(0, f.alias.specQueries);
    }
}
