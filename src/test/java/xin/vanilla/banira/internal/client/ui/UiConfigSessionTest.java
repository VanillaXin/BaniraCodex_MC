package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.platform.*;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiConfigSessionTest {
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }
    enum Mode { FIRST, SECOND }

    static final class Fixture {
        final UiConfigParityTest.Store store = new UiConfigParityTest.Store();
        final List<ConfigEntryDescriptor> descriptors = new ArrayList<>();
        final List<Map<String, String>> sent = new ArrayList<>();
        final AtomicInteger fetches = new AtomicInteger(), saves = new AtomicInteger();
        final List<String> notices = new ArrayList<>();
        final Map<String, ConfigCategoryTitleSpec> titles = new LinkedHashMap<>();
        ConfigHolder holder;
        Object session;
        boolean connected = true, failSend, failFetch;

        void field(String path, ConfigEntryDescriptor.ConfigValueType kind, Object value) {
            store.values.put(path, value);
            descriptors.add(ConfigEntryDescriptor.builder().path(path).displayName(path)
                    .tooltip(Collections.singletonList("Complete description " + path))
                    .valueType(kind).defaultValue(value).enumClass(Mode.class).build());
        }
        void open() throws Exception {
            holder = ConfigHolder.create("test", "test-common.toml", ConfigScope.COMMON, store,
                    descriptors, Collections.emptyMap(), titles);
            holder.onSaved(paths -> saves.incrementAndGet());
            Class<?> sessionType = type("internal.client.ui.page.ConfigUiSession");
            Class<?> services = type("internal.client.ui.page.ConfigUiSession$Services");
            Object port = Proxy.newProxyInstance(services.getClassLoader(), new Class<?>[]{services}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "connected": return connected;
                    case "sync":
                        if (failSend) throw new IllegalStateException("send rejected");
                        sent.add(new LinkedHashMap<>((Map<String, String>) args[1])); return null;
                    case "fetch":
                        if (failFetch) throw new SecurityException("read denied");
                        fetches.incrementAndGet(); return null;
                    case "notify": notices.add((String) args[0]); return null;
                    case "execute": ((Runnable) args[0]).run(); return null;
                    default: return null;
                }
            });
            session = sessionType.getConstructor(ConfigHolder.class, ConfigEditorScreen.Args.class, services)
                    .newInstance(holder, new ConfigEditorScreen.Args(), port);
        }
        void draft(String path, String text) throws Exception { call(session, "draft", new Class<?>[]{String.class, String.class}, path, text); }
        Object value(String path) throws Exception { return call(session, "value", new Class<?>[]{String.class}, path); }
    }

    @Test public void descriptorsRetainExactScalarAndListTypesWithoutNativeWidgets() throws Exception {
        Fixture f = new Fixture();
        f.field("group.long", ConfigEntryDescriptor.ConfigValueType.LONG, 1L);
        f.field("group.enum", ConfigEntryDescriptor.ConfigValueType.ENUM, Mode.FIRST);
        f.field("group.list", ConfigEntryDescriptor.ConfigValueType.STRING_LIST, Arrays.asList("a,b", "a,b"));
        f.open();
        f.draft("group.long", "9007199254740993");
        f.draft("group.enum", "SECOND");
        f.draft("group.list", "[\"x,y\",\"x,y\",\"\"]");
        assertEquals(9007199254740993L, f.value("group.long"));
        assertEquals(Mode.SECOND, f.value("group.enum"));
        assertEquals(Arrays.asList("x,y", "x,y", ""), f.value("group.list"));
        xin.vanilla.banira.internal.client.ConfigEditorState state =
                (xin.vanilla.banira.internal.client.ConfigEditorState) call(f.session, "state");
        for (var entry : state.entryWidgets().values()) assertNull(entry.getWidget());
        assertEquals(3, state.pendingChangeCount());
    }

    @Test public void invalidDraftSurvivesBindingAndReloadAndBlocksSave() throws Exception {
        Fixture f = new Fixture();
        f.field("group.number", ConfigEntryDescriptor.ConfigValueType.INTEGER, 2);
        f.open(); f.draft("group.number", "-");
        NativeUiView old = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, old);
        call(f.session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.RELOAD);
        f.store.values.put("group.number", 3);
        f.holder.acceptExternalReload();
        NativeUiView next = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, next);
        assertEquals("-", call(f.session, "draft", new Class<?>[]{String.class}, "group.number"));
        assertEquals(false, call(f.session, "save"));
        assertEquals(0, f.saves.get());
        assertEquals(0, old.subscriptionCount());
    }

    @Test public void resetChangesOnlyDraftUntilSuccessfulSave() throws Exception {
        Fixture f = new Fixture();
        f.field("name", ConfigEntryDescriptor.ConfigValueType.STRING, "default");
        f.store.values.put("name", "saved");
        f.open();
        call(f.session, "reset", new Class<?>[]{String.class}, "name");
        assertEquals("saved", f.holder.get("name"));
        assertEquals("default", f.value("name"));
        assertEquals(true, call(f.session, "save"));
        assertEquals("default", f.holder.get("name"));
        assertEquals(1, f.saves.get());
        assertEquals(true, call(f.session, "save"));
        assertEquals(1, f.saves.get());
    }

    @Test public void syncUsesTouchedOrAllValuesAndFetchNeverWritesLocalHolder() throws Exception {
        Fixture f = new Fixture();
        f.field("one", ConfigEntryDescriptor.ConfigValueType.STRING, "a");
        f.field("two", ConfigEntryDescriptor.ConfigValueType.INTEGER, 2);
        f.open(); f.draft("one", "b");
        call(f.session, "sync", new Class<?>[]{boolean.class}, false);
        assertEquals(Collections.singletonMap("one", "b"), f.sent.get(0));
        call(f.session, "sync", new Class<?>[]{boolean.class}, true);
        assertEquals(2, f.sent.get(1).size());
        call(f.session, "fetch"); assertEquals(1, f.fetches.get());
        call(f.session, "acceptRemoteSnapshot", new Class<?>[]{String.class, Map.class},
                f.holder.getConfigName(), Collections.singletonMap("one", "remote"));
        assertEquals("a", f.holder.get("one"));
        f.draft("one", "remote edit");
        call(f.session, "save");
        assertEquals(Collections.singletonMap("one", "remote edit"), f.sent.get(2));
        assertEquals("a", f.holder.get("one"));
        f.connected = false; call(f.session, "fetch");
        assertEquals(1, f.fetches.get());
    }

    @Test public void failedSyncDoesNotCleanDraftAndClosedSessionIgnoresOldActions() throws Exception {
        Fixture f = new Fixture();
        f.field("one", ConfigEntryDescriptor.ConfigValueType.STRING, "a");
        f.open(); f.draft("one", "b"); f.failSend = true;
        call(f.session, "sync", new Class<?>[]{boolean.class}, false);
        var state = (xin.vanilla.banira.internal.client.ConfigEditorState) call(f.session, "state");
        assertEquals(1, state.pendingChangeCount());
        call(f.session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        f.failSend = false;
        call(f.session, "sync", new Class<?>[]{boolean.class}, false);
        assertTrue(f.sent.isEmpty());
    }

    @Test public void queuedSyncKeepsRetryableDraftUntilAuthoritativeSnapshotConfirmsIt() throws Exception {
        Fixture f = new Fixture(); f.field("one", ConfigEntryDescriptor.ConfigValueType.STRING, "a");
        f.open(); f.draft("one", "b");
        assertEquals(true, call(f.session, "sync", new Class<?>[]{boolean.class}, false));
        var state = (xin.vanilla.banira.internal.client.ConfigEditorState) call(f.session, "state");
        assertEquals("A queued packet is not a server acknowledgement", 1, state.pendingChangeCount());
        assertEquals(0, f.fetches.get());
        call(f.session, "acceptRemoteSnapshot", new Class<?>[]{String.class, Map.class},
                f.holder.getConfigName(), Map.of("one", "a"));
        assertEquals("b", f.value("one")); assertEquals(1, state.pendingChangeCount());
        assertEquals(true, call(f.session, "save"));
        assertEquals(Map.of("one", "b"), f.sent.get(1));
        call(f.session, "acceptRemoteSnapshot", new Class<?>[]{String.class, Map.class},
                f.holder.getConfigName(), Map.of("one", "b"));
        assertEquals(0, state.pendingChangeCount());
        assertEquals("a", f.holder.get("one"));
        assertEquals(0, f.saves.get());
    }

    @Test public void snapshotForEarlierSyncCannotDiscardNewerOrInvalidEdits() throws Exception {
        Fixture f = new Fixture(); f.field("one", ConfigEntryDescriptor.ConfigValueType.INTEGER, 1);
        f.open(); f.draft("one", "2");
        call(f.session, "sync", new Class<?>[]{boolean.class}, false);
        f.draft("one", "3");
        call(f.session, "acceptRemoteSnapshot", new Class<?>[]{String.class, Map.class},
                f.holder.getConfigName(), Map.of("one", 2));
        assertEquals(3, f.value("one"));
        assertEquals(1, ((xin.vanilla.banira.internal.client.ConfigEditorState) call(f.session, "state")).pendingChangeCount());
        f.draft("one", "-");
        call(f.session, "acceptRemoteSnapshot", new Class<?>[]{String.class, Map.class},
                f.holder.getConfigName(), Map.of("one", 3));
        assertEquals("-", call(f.session, "draft", new Class<?>[]{String.class}, "one"));
        assertEquals(false, call(f.session, "save"));
        assertEquals(1, f.sent.size());
    }

    @Test public void fieldWriteDoesNotDependOnPermissionToFetchTheWholeConfiguration() throws Exception {
        Fixture f = new Fixture(); f.field("one", ConfigEntryDescriptor.ConfigValueType.STRING, "a");
        f.open(); f.draft("one", "b"); f.failFetch = true;
        assertEquals(true, call(f.session, "sync", new Class<?>[]{boolean.class}, false));
        assertEquals(List.of(Map.of("one", "b")), f.sent);
        assertEquals(0, f.fetches.get());
        var state = (xin.vanilla.banira.internal.client.ConfigEditorState) call(f.session, "state");
        assertEquals(1, state.pendingChangeCount());
        call(f.session, "acceptRemoteSnapshot", new Class<?>[]{String.class, Map.class},
                f.holder.getConfigName(), Map.of("one", "b"));
        assertEquals(0, state.pendingChangeCount());
        assertEquals("a", f.holder.get("one"));
    }

    @Test public void escapeProtectsDirtySessionButDisconnectReleasesIt() throws Exception {
        Fixture f = new Fixture();
        f.field("one", ConfigEntryDescriptor.ConfigValueType.STRING, "a");
        f.open(); f.draft("one", "unsaved");
        List<Object> shown = new ArrayList<>();
        UiSessionHost host = new UiSessionHost(new UiBackendController(() -> true, () -> true),
                (page, previous) -> new NativeUiView(new Object()), shown::add, screen -> true);
        UiPageSpec page = new UiPageSpec("banira_codex:config", "banira_codex/ui/config.html",
                (UiPageSession) f.session, previous -> new Object());
        assertTrue(host.open(page, null));
        NativeUiView view = host.view();
        host.close(UiCloseReason.USER_CLOSE);
        assertSame(page, host.page());
        assertFalse(view.closed());
        host.close(UiCloseReason.DISCONNECT);
        assertNull(host.page());
        assertTrue(view.closed());
    }
}
