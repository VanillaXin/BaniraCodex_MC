package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.notification.NotificationHistory;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiNotificationPagesTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    static Class<?> sessionType() {
        try {
            return Class.forName("xin.vanilla.banira.internal.client.ui.page.NotificationUiSession");
        } catch (ClassNotFoundException missing) {
            throw new AssertionError("Notification pages need shared state", missing);
        }
    }

    static class Fixture {
        final NotificationHistory history = new NotificationHistory();
        Map<String, NotificationTypeSettingsStore.TypeSettings> settings = new LinkedHashMap<>();
        int marks, allMarks, saves;
        double x = .25, y = .75;
        boolean failSave;

        Object session(String portName) throws Exception {
            Class<?> type = sessionType(), port = Class.forName(type.getName() + "$" + portName);
            Object proxy = Proxy.newProxyInstance(port.getClassLoader(), new Class<?>[]{port}, (owner, method, args) -> {
                switch (method.getName()) {
                    case "entries":
                        return history.snapshot();
                    case "revision":
                        return history.revision();
                    case "unread":
                        return history.unreadCount();
                    case "markRead":
                        marks++;
                        return history.markRead((Long) args[0]);
                    case "markAllRead":
                        allMarks++;
                        return history.markAllRead();
                    case "snapshot":
                        return settings;
                    case "knownTypes":
                        return Arrays.asList("test.first", "test.second");
                    case "x":
                        return x;
                    case "y":
                        return y;
                    case "save":
                        if (failSave) throw new IllegalStateException("write rejected");
                        saves++;
                        if (args.length == 1)
                            settings = (Map<String, NotificationTypeSettingsStore.TypeSettings>) args[0];
                        else {
                            x = (Double) args[0];
                            y = (Double) args[1];
                        }
                        return null;
                    default:
                        return null;
                }
            });
            return type.getConstructor(port).newInstance(proxy);
        }
    }

    @Test
    public void explicitEntryOpeningMarksReadWhileBackendRestorationDoesNot() throws Exception {
        Fixture fixture = new Fixture();
        fixture.history.append(new NotificationLogEntry().id(9), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) fixture.session("HistoryPort");
        session.select(9);
        assertEquals(1, fixture.marks);
        assertEquals(0, fixture.history.unreadCount());
        session.restoreSelection(9);
        assertEquals(1, fixture.marks);
        String source = java.nio.file.Files.readString(java.nio.file.Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/NotificationLogScreen.java"), java.nio.charset.StandardCharsets.UTF_8);
        String constructor = source.substring(source.indexOf("public NotificationLogScreen(Args args)"), source.indexOf("    @Data"));
        assertTrue(constructor.contains("uiSession.select(pendingSelectLogEntryId)"));
        assertFalse(constructor.contains("uiSession.restoreSelection(pendingSelectLogEntryId)"));
    }

    @Test
    public void nativeTypeSearchUpdatesSessionBeforeAnyWidgetRebuild() throws Exception {
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) new Fixture().session("SettingsPort");
        session.query("native draft");
        session.detach(UiDetachReason.RELOAD);
        assertEquals("native draft", session.query());
        String source = java.nio.file.Files.readString(java.nio.file.Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/NotificationTypeConfigScreen.java"), java.nio.charset.StandardCharsets.UTF_8);
        String handler = source.substring(source.indexOf("private void applySearchFilter(String value)"));
        assertTrue(handler.indexOf("uiSession.query(searchText)") > handler.indexOf("searchText ="));
        assertTrue(handler.indexOf("uiSession.query(searchText)") < handler.indexOf("if (contentRootPanel == null)"));
    }

    @Test
    public void parentRestoreSurvivesBackendDetachAndRunsOnlyOnceAfterClose() throws Exception {
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) new Fixture().session("HistoryPort");
        List<Object> restored = new ArrayList<>();
        Object parent = new Object();
        session.onParentReturn(restored::add);
        session.afterParentReturn(parent);
        assertTrue(restored.isEmpty());
        session.detach(UiDetachReason.SWITCH);
        session.close(UiCloseReason.CANCEL);
        session.afterParentReturn(parent);
        session.afterParentReturn(parent);
        session.onParentReturn(restored::add);
        session.afterParentReturn(parent);
        assertEquals(Collections.singletonList(parent), restored);
    }

    @Test
    public void disconnectDropsThePendingParentRestore() throws Exception {
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) new Fixture().session("HistoryPort");
        List<Object> restored = new ArrayList<>();
        session.onParentReturn(restored::add);
        session.close(UiCloseReason.DISCONNECT);
        session.afterParentReturn(new Object());
        assertTrue(restored.isEmpty());
    }

    @Test
    public void historySearchRemainsOneTrimmedSubstring() {
        var entry = new NotificationLogEntry().id(7).source("network").notificationType("test.alpha.beta")
                .componentJson(xin.vanilla.banira.common.data.AbstractComponent.serialize(
                        xin.vanilla.banira.BaniraComponent.get().literal("Alpha other Beta")).toString());
        assertTrue(xin.vanilla.banira.internal.client.ui.page.NotificationUiSession.matches(entry, "other"));
        assertTrue(xin.vanilla.banira.internal.client.ui.page.NotificationUiSession.matches(entry, " NETWORK "));
        assertTrue(xin.vanilla.banira.internal.client.ui.page.NotificationUiSession.matches(entry, "alpha.beta"));
        assertFalse(xin.vanilla.banira.internal.client.ui.page.NotificationUiSession.matches(entry, "alpha beta"));
        assertTrue(xin.vanilla.banira.internal.client.ui.page.NotificationUiSession.matches(entry, "  "));
    }

    @Test
    public void styledHistorySummaryUsesReadablePlainText() throws Exception {
        var component = xin.vanilla.banira.BaniraComponent.get().literal("Before")
                .color(xin.vanilla.banira.common.data.Color.argb(0xffffaa00)).append(
                        xin.vanilla.banira.BaniraComponent.get().literal(" Styled").color(xin.vanilla.banira.common.data.Color.argb(0xffffffff)));
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1)
                .componentJson(xin.vanilla.banira.common.data.AbstractComponent.serialize(component).toString()), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        session.bind(root);
        root.rowProviders.get("fields").bind(row, 0);
        assertEquals("\u2022 Before Styled", row.texts.get("open").toVanilla().getString());
    }

    @Test
    public void filteredMarkAllIncludesHiddenHistory() throws Exception {
        Fixture f = new Fixture();
        for (long id = 1; id <= 3; id++)
            f.history.append(new NotificationLogEntry().id(id).notificationType("test." + id), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        session.bind(new NativeUiView(new Object()));
        session.query("test.1");
        assertEquals(1, session.filteredEntries().size());
        session.markAllRead();
        session.markAllRead();
        assertEquals(1, f.allMarks);
        assertEquals(0, f.history.unreadCount());
        assertTrue(f.history.snapshot().stream().allMatch(NotificationLogEntry::read));
    }

    @Test
    public void trimmedSelectionClearsDetailAndRetiredAction() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1), 1);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        NativeUiView row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, 0);
        session.restoreSelection(1);
        f.history.append(new NotificationLogEntry().id(2), 1);
        session.tick();
        assertEquals(0, session.selectedId());
        assertTrue(root.texts.get("text").isEmpty());
        row.activate("open");
        assertEquals(0, f.marks);
    }

    @Test
    public void historyRowsUseFixedPresentationAndRetainReadStyle() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(4).styleName("WARNING"), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        assertTrue(root.rowProviders.get("fields") instanceof UiRowPresentation);
        UiRowPresentation rows = (UiRowPresentation) root.rowProviders.get("fields");
        assertEquals(48, rows.rowHeight());
        assertTrue(rows.rowClass(0).contains("unread"));
        session.select(4);
        assertTrue(((UiRowPresentation) root.rowProviders.get("fields")).rowClass(0).contains("selected"));
    }

    @Test
    public void selectingAlreadyReadHistoryRefreshesItsHighlight() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1).read(true), 10);
        f.history.append(new NotificationLogEntry().id(2).read(true), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        session.select(1);
        assertTrue(((UiRowPresentation) root.rowProviders.get("fields")).rowClass(1).contains("selected"));
        session.select(2);
        assertTrue(((UiRowPresentation) root.rowProviders.get("fields")).rowClass(0).contains("selected"));
        assertFalse(((UiRowPresentation) root.rowProviders.get("fields")).rowClass(1).contains("selected"));
    }

    @Test
    public void alreadyReadSelectionRefreshesRealizedDomClass() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1).read(true), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        session.bind(dom.view());
        session.select(1);
        assertTrue(dom.created.stream().anyMatch(node -> !node.removed
                && node.attributes.getOrDefault("class", "").contains("log-row")
                && node.attributes.getOrDefault("class", "").contains("selected")));
    }

    @Test
    public void historyStyleClassesCannotCollideWithGlobalValidation() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1).styleName("ERROR"), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object());
        session.bind(root);
        assertFalse(Arrays.asList(((UiRowPresentation) root.rowProviders.get("fields")).rowClass(0).split(" ")).contains("error"));
    }

    @Test
    public void compactHistoryUsesOneConsistentVirtualRowHeight() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object()) {
            @Override
            public xin.vanilla.banira.client.data.ScreenCoordinate bounds(String id) {
                return new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 320, 100);
            }
        };
        session.bind(root);
        UiRowPresentation rows = (UiRowPresentation) root.rowProviders.get("fields");
        assertEquals(24, rows.rowHeight());
        assertTrue(rows.rowClass(0).contains("compact"));
    }

    @Test
    public void bindSwitchAndRestoreSelectionNeverMarkHistoryRead() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1).notificationType("test.first"), 10);
        f.history.append(new NotificationLogEntry().id(2).notificationType("test.second"), 10);
        Object session = f.session("HistoryPort");
        call(session, "bind", new Class<?>[]{UiView.class}, new NativeUiView(new Object()));
        assertEquals(2, f.history.unreadCount());
        call(session, "restoreSelection", new Class<?>[]{long.class}, 1L);
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        call(session, "bind", new Class<?>[]{UiView.class}, new NativeUiView(new Object()));
        assertEquals(1L, call(session, "selectedId"));
        assertEquals(0, f.marks);
        call(session, "select", new Class<?>[]{long.class}, 1L);
        assertEquals(1, f.marks);
        assertEquals(1, f.history.unreadCount());
        call(session, "markAllRead");
        call(session, "markAllRead");
        assertEquals(1, f.allMarks);
        assertEquals(0, f.history.unreadCount());
    }

    @Test
    public void historyUsesStableIdsAndReleasesRecycledRows() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1).notificationType("test.first"), 10);
        Object session = f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, root);
        var provider = root.rowProviders.get("fields");
        assertEquals("notification:1", provider.key(0));
        NativeUiView row = new NativeUiView(new Object());
        provider.bind(row, 0);
        call(session, "restoreSelection", new Class<?>[]{long.class}, 1L);
        f.history.append(new NotificationLogEntry().id(2).notificationType("test.second"), 10);
        call(session, "tick");
        assertEquals(1L, call(session, "selectedId"));
        assertEquals("notification:2", root.rowProviders.get("fields").key(0));
        row.activate("open");
        assertEquals(0, f.marks);
        call(session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        assertEquals(0, root.subscriptionCount());
    }

    @Test
    public void settingsDraftIsCopiedAndCancelNeverPersists() throws Exception {
        Fixture f = new Fixture();
        f.settings.put("test.first", new NotificationTypeSettingsStore.TypeSettings().durationMs(9000));
        Object session = f.session("SettingsPort");
        var original = (NotificationTypeSettingsStore.TypeSettings) call(session, "settings", new Class<?>[]{String.class}, "test.first");
        original.hidden(true);
        assertFalse(f.settings.get("test.first").hidden());
        call(session, "settings", new Class<?>[]{String.class, NotificationTypeSettingsStore.TypeSettings.class}, "test.first", original);
        assertEquals(1, call(session, "changedCount"));
        assertEquals(false, call(session, "canClose", new Class<?>[]{UiCloseReason.class}, UiCloseReason.USER_CLOSE));
        call(session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.CANCEL);
        assertEquals(0, f.saves);
        assertFalse(f.settings.get("test.first").hidden());
    }

    @Test
    public void failedSettingsSaveRetainsDraftAndSuccessfulSaveMovesBaseline() throws Exception {
        Fixture f = new Fixture();
        Object session = f.session("SettingsPort");
        call(session, "settings", new Class<?>[]{String.class, NotificationTypeSettingsStore.TypeSettings.class}, "test.first", new NotificationTypeSettingsStore.TypeSettings().animationName("").positionName("").durationMs(0).hidden(true));
        f.failSave = true;
        assertEquals(false, call(session, "save"));
        assertEquals(1, call(session, "changedCount"));
        f.failSave = false;
        assertEquals(true, call(session, "save"));
        assertEquals(0, call(session, "changedCount"));
        assertTrue(f.settings.get("test.first").hidden());
        assertEquals(1, f.saves);
    }

    @Test
    public void positionPreviewClampsAndOnlyExplicitSavePersists() throws Exception {
        Fixture f = new Fixture();
        Object session = f.session("PositionPort");
        call(session, "moveTo", new Class<?>[]{double.class, double.class}, -1.0, 2.0);
        assertEquals(0.0, (Double) call(session, "x"), 0);
        assertEquals(1.0, (Double) call(session, "y"), 0);
        assertEquals(.25, f.x, 0);
        assertEquals(0, f.saves);
        assertEquals(false, call(session, "canClose", new Class<?>[]{UiCloseReason.class}, UiCloseReason.USER_CLOSE));
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        call(session, "bind", new Class<?>[]{UiView.class}, new NativeUiView(new Object()));
        assertEquals(1.0, (Double) call(session, "y"), 0);
        assertEquals(true, call(session, "save"));
        assertEquals(1, f.saves);
        assertEquals(0, f.marks);
        assertEquals(true, call(session, "canClose", new Class<?>[]{UiCloseReason.class}, UiCloseReason.USER_CLOSE));
    }

    @Test
    public void allNativeNotificationEntriesExposeSharedRouting() throws Exception {
        for (String name : new String[]{"NotificationLogScreen", "NotificationTypeConfigScreen", "NotificationHudPositionScreen"}) {
            Class<?> type = Class.forName("xin.vanilla.banira.client.gui." + name);
            assertTrue(name, UiNativePage.class.isAssignableFrom(type));
        }
    }

    @Test
    public void newHistoryRecordsKeepTheTopRecordIdentityAndFraction() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1), 10);
        Object session = f.session("HistoryPort");
        NativeUiView root = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, root);
        root.state().scrollAnchor("notification:1");
        root.state().rowPosition(.25);
        f.history.append(new NotificationLogEntry().id(2), 10);
        call(session, "tick");
        assertEquals(1.25, root.state().rowPosition(), 0);
        assertEquals("notification:1", root.state().scrollAnchor());
        assertEquals(0, f.marks);
    }

    @Test
    public void invalidDurationAndInheritanceSurviveSwitchUntilCorrected() throws Exception {
        Fixture f = new Fixture();
        Object session = f.session("SettingsPort");
        NativeUiView root = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, root);
        var provider = root.rowProviders.get("fields");
        NativeUiView duration = new NativeUiView(new Object());
        provider.bind(duration, 1);
        duration.inputDraft("input", "-");
        assertEquals(false, call(session, "save"));
        assertEquals(0, f.saves);
        call(session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        NativeUiView next = new NativeUiView(new Object());
        call(session, "bind", new Class<?>[]{UiView.class}, next);
        NativeUiView restored = new NativeUiView(new Object());
        next.rowProviders.get("fields").bind(restored, 1);
        assertEquals("-", restored.drafts.get("input"));
        NativeUiView animation = new NativeUiView(new Object());
        next.rowProviders.get("fields").bind(animation, 2);
        assertEquals("", animation.controls.get("input").options().get(0).value());
        assertEquals("", animation.values.get("input").asText());
        restored.inputDraft("input", "0");
        assertEquals(true, call(session, "save"));
        assertEquals(1, f.saves);
        assertEquals(0, call(session, "changedCount"));
    }

    @Test
    public void realHostSwitchAndReloadKeepTheSameHistoryAndDetailState() throws Exception {
        Fixture f = new Fixture();
        f.history.append(new NotificationLogEntry().id(1), 10);
        var session = (xin.vanilla.banira.internal.client.ui.page.NotificationUiSession) f.session("HistoryPort");
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        UiSessionHost host = new UiSessionHost(backend, (page, previous) -> new NativeUiView(new Object()), value -> {
        }, value -> true);
        UiPageSpec page = new UiPageSpec("banira_codex:notification-log", "banira_codex/ui/notification-log.html", session,
                previous -> new Object());
        assertTrue(host.open(page, null));
        session.restoreSelection(1);
        session.query("default");
        UiViewState state = host.view().state();
        state.detailScroll(77);
        state.scrollAnchor("notification:1");
        for (int i = 0; i < 10; i++) {
            NativeUiView old = host.view();
            host.switchBackend(i % 2 != 0);
            host.reload();
            host.tick();
            assertSame(state, host.view().state());
            assertEquals(77, state.detailScroll(), 0);
            assertEquals(1, session.selectedId());
            assertEquals(1, f.history.unreadCount());
            assertEquals(0, f.marks);
            assertEquals(0, old.subscriptionCount());
        }
        host.disconnect();
        assertNull(host.view());
        assertEquals(1, f.history.unreadCount());
    }
}
