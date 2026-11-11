package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiPointer;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.internal.client.ui.page.NotificationUiSession;
import xin.vanilla.banira.internal.compat.aui.AuiGameElements;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.*;

public class UiNotificationInteractionTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.util.registry.Bootstrap.bootStrap();
    }

    @Test
    public void historyDetailModeIsScopedToItsWorkspace() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        lombok.var workspace = dom.new Node("history_workspace");
        dom.ids.put("history_workspace", workspace);
        NativeUiView view = dom.view();
        view.value("history_detail", UiValue.bool(true));
        assertEquals("notification-columns detail-open", workspace.attributes.get("class"));
        view.value("history_detail", UiValue.bool(false));
        assertEquals("notification-columns", workspace.attributes.get("class"));
        assertFalse(dom.ids.get("body").attributes.containsKey("class"));
    }

    @Test
    public void historyBackAndClearDoNotReadAnotherRecord() throws Exception {
        lombok.var f = new UiNotificationPagesTest.Fixture();
        f.history.append(new xin.vanilla.banira.client.data.NotificationLogEntry().id(1), 10);
        lombok.var session = (NotificationUiSession) f.session("HistoryPort");
        lombok.var view = new NativeUiView(new Object());
        session.bind(view);
        session.select(1);
        view.activate("back");
        assertEquals(1, session.selectedId());
        assertEquals(1, f.marks);
        assertNotNull(view.values.get("history_detail"));
        assertFalse(view.values.get("history_detail").asBoolean());
        session.query("missing");
        view.activate("clear_search");
        assertEquals("", session.query());
        assertEquals(1, f.marks);
    }

    @Test
    public void historyMarkupRemainsLiteralComponentAndOldActionsRetire() throws Exception {
        lombok.var f = new UiNotificationPagesTest.Fixture();
        f.history.append(new xin.vanilla.banira.client.data.NotificationLogEntry().id(1)
                .componentJson(xin.vanilla.banira.common.data.AbstractComponent.serialize(
                        xin.vanilla.banira.BaniraComponent.get().literal("<b>literal</b>").clickEvent(
                                new net.minecraft.util.text.event.ClickEvent(net.minecraft.util.text.event.ClickEvent.Action.SUGGEST_COMMAND, "/list"))).toString()), 10);
        lombok.var session = (NotificationUiSession) f.session("HistoryPort");
        lombok.var old = new NativeUiView(new Object());
        session.bind(old);
        session.restoreSelection(1);
        assertEquals("<b>literal</b>", old.texts.get("text").toString());
        assertNotNull(old.texts.get("text").toVanilla().getStyle().getClickEvent());
        session.detach(UiDetachReason.SWITCH);
        session.bind(new NativeUiView(new Object()));
        old.activate("mark_all_read");
        old.activate("types");
        old.activate("position");
        assertEquals(0, f.allMarks);
        assertEquals(0, f.marks);
        assertEquals(0, old.subscriptionCount());
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void previewDragUsesOriginalPixelSpaceAndLosesCaptureOnSwitch() throws Exception {
        UiNotificationPagesTest.Fixture f = new UiNotificationPagesTest.Fixture();
        NotificationUiSession session = (NotificationUiSession) f.session("PositionPort");
        NativeUiView view = new NativeUiView(new Object()) {
            @Override
            public ScreenCoordinate bounds(String id) {
                return id.equals("viewport") ? new ScreenCoordinate(0, 0, 400, 300) : new ScreenCoordinate(50, 60, 40, 20);
            }
        };
        session.bind(view);
        view.pointer("preview", new UiPointer(UiPointer.Kind.DOWN, 55, 65, 1));
        view.pointer("preview", new UiPointer(UiPointer.Kind.MOVE, 399, 299, 0));
        assertEquals(.25, session.x(), 0);
        view.pointer("preview", new UiPointer(UiPointer.Kind.DOWN, 55, 65, 0));
        view.pointer("preview", new UiPointer(UiPointer.Kind.MOVE, 399, 299, 0));
        assertEquals(1, session.x(), 0);
        assertEquals(1, session.y(), 0);
        assertEquals(0, f.saves);
        session.detach(UiDetachReason.SWITCH);
        view.pointer("preview", new UiPointer(UiPointer.Kind.MOVE, 0, 0, 0));
        assertEquals(1, session.x(), 0);
        assertEquals(0, view.subscriptionCount());
        session.moveTo(Double.NaN, 0);
        assertEquals(1, session.x(), 0);
    }

    @Test
    public void richTextHitTestRejectsOutsideDetailAndOffscreenLines() {
        ScreenCoordinate text = new ScreenCoordinate(100, -100, 200, 900);
        ScreenCoordinate clip = new ScreenCoordinate(100, 50, 200, 100);
        assertEquals(-1, AuiGameElements.lineAt(text, clip, 99, 70, 9, 100));
        assertEquals(-1, AuiGameElements.lineAt(text, clip, 110, 150, 9, 100));
        assertEquals(17, AuiGameElements.lineAt(text, clip, 110, 53, 9, 100));
        assertEquals(-1, AuiGameElements.lineAt(text, clip, Double.NaN, 53, 9, 100));
    }

    @Test
    public void independentDetailScrollDoesNotMoveListAndReleasesListener() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        UiDomControlsTest.Dom.Node detail = dom.new Node("detail");
        dom.ids.put("detail", detail);
        UiDomView view = (UiDomView) dom.view();
        view.state().rowPosition(8);
        view.state().detailScroll(123);
        view.afterLayout();
        assertEquals(123, detail.scroll, 0);
        detail.scroll = 200;
        detail.emit("scroll");
        assertEquals(200, view.state().detailScroll(), 0);
        assertEquals(8, view.state().rowPosition(), 0);
        view.close();
        assertTrue(detail.events.values().stream().allMatch(java.util.List::isEmpty));
    }

    @Test
    public void disablingMarkAllImmediatelyRemovesItsTooltipAndRestoresItWhenEnabled() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        UiDomControlsTest.Dom.Node button = dom.new Node("mark_all_read");
        dom.ids.put("mark_all_read", button);
        NativeUiView view = dom.view();
        view.tooltip("mark_all_read", xin.vanilla.banira.BaniraComponent.get().literal("Mark all read"));
        assertEquals("Mark all read", button.tooltip);
        view.enabled("mark_all_read", false);
        assertEquals("", button.tooltip);
        view.enabled("mark_all_read", true);
        assertEquals("Mark all read", button.tooltip);
    }

    @Test
    public void pendingSelectionScrollResetSurvivesImmediateViewClose() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        UiDomControlsTest.Dom.Node detail = dom.new Node("detail");
        dom.ids.put("detail", detail);
        UiDomView view = (UiDomView) dom.view();
        view.state().detailScroll(100);
        view.afterLayout();
        view.state().detailScroll(0);
        view.close();
        assertEquals(0, view.state().detailScroll(), 0);
    }

    @Test
    public void positionSaveControlOnlyPersistsAndClosesOnce() throws Exception {
        UiNotificationPagesTest.Fixture f = new UiNotificationPagesTest.Fixture();
        NotificationUiSession session = (NotificationUiSession) f.session("PositionPort");
        NativeUiView view = new NativeUiView(new Object());
        java.util.concurrent.atomic.AtomicInteger closes = new java.util.concurrent.atomic.AtomicInteger();
        view.onClose(() -> {
            closes.incrementAndGet();
            session.close(UiCloseReason.SUBMIT);
        });
        session.moveTo(.6, .4);
        session.bind(view);
        view.activate("save");
        view.activate("save");
        assertEquals(1, f.saves);
        assertEquals(1, closes.get());
        assertEquals(.6, f.x, 0);
        assertEquals(.4, f.y, 0);
        assertEquals(0, view.subscriptionCount());
    }
}
