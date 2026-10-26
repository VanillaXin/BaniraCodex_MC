package xin.vanilla.banira.internal.client.ui;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.util.FormattedCharSequence;
import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.component.Notification;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

public class UiNotificationHudPresentationTest {
    @org.junit.BeforeClass public static void bootstrap() { UiDomControlsTest.bootstrap(); }
    @org.junit.Before public void platform() { new UiDomControlsTest().platform(); }
    @Test public void visualRunsRetainUnicodeAndOriginalActionWithoutHtmlParsing() {
        Style action = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/list"));
        FormattedCharSequence sequence = FormattedCharSequence.composite(
                FormattedCharSequence.forward("<tag> ", Style.EMPTY),
                FormattedCharSequence.forward("\ud83c\udf38 action", action));
        var runs = NotificationHudPresentation.runs(sequence);
        assertEquals(List.of(new NotificationHudPresentation.Run("<tag> ", Style.EMPTY),
                new NotificationHudPresentation.Run("\ud83c\udf38 action", action)), runs);
        assertSame(action, runs.get(1).style());
    }

    @Test public void unreadUsesRealCountWithoutInstallingAnInputHandlerAndRemovesStaleNodes() throws Exception {
        var dom = new UiDomControlsTest.Dom(); dom.ids.put("overlay", dom.new Node("div"));
        var presentation = new HudDomPresentation((UiDomAccess) dom.access, b -> new ScreenCoordinate(b.x() * 2, b.y() * 2, b.width() * 2, b.height() * 2));
        HudDrawList frame = new HudDrawList(); frame.unread(10, 20, 150, BaniraColorConfig.forSeason(xin.vanilla.banira.common.enums.EnumSeason.SPRING));
        presentation.update(frame);
        assertEquals(3, dom.created.size());
        assertEquals("99+", dom.created.get(2).text);
        assertTrue(dom.created.get(0).attributes.get("style").contains("width:64.0px"));
        int writes = dom.created.stream().mapToInt(n -> n.writes).sum();
        presentation.update(frame);
        assertEquals(writes, dom.created.stream().mapToInt(n -> n.writes).sum());
        assertTrue(dom.created.stream().allMatch(n -> n.events.isEmpty()));
        presentation.update(new HudDrawList());
        assertTrue(dom.created.stream().allMatch(n -> n.removed)); presentation.close();
    }

    @Test public void styledHitTestingUsesVisibleDomFragmentsAndDoesNotFallBackToVanillaMetrics() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var field = unsafeType.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Notification n = (Notification) unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), Notification.class);
        Style action = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/list"));
        n.auiHitLayout(new ScreenCoordinate(0, 0, 120, 40), new ScreenCoordinate(105, 4, 11, 11),
                new ScreenCoordinate(6, 18, 108, 18), List.of(new Notification.TextHit(new ScreenCoordinate(20, 18, 30, 10), action)));
        assertSame(action, n.styleAtTextPoint(25, 22));
        assertNull(n.styleAtTextPoint(60, 22));
        assertNull(n.styleAtTextPoint(25, 5));
        assertTrue(n.isCloseHit(108, 8));
        assertFalse(n.isCloseHit(80, 8));
        n.auiHitLayout(new ScreenCoordinate(0, 0, 120, 40), new ScreenCoordinate(105, 4, 11, 11),
                new ScreenCoordinate(6, 18, 108, 18), List.of());
        assertNull(n.styleAtTextPoint(25, 22));
    }

    @Test public void bubbleUsesActualModuleAndStyledRunsWithoutReplacingManagerInputOwnership() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var f = unsafeType.getDeclaredField("theUnsafe"); f.setAccessible(true);
        Notification n = (Notification) unsafeType.getMethod("allocateInstance", Class.class).invoke(f.get(null), Notification.class);
        n.notificationType("banira_codex:test"); n.cachedWidth(120); n.cachedHeight(42); n.renderAlpha(-1); n.renderScale(1);
        var aui = Notification.class.getDeclaredField("auiLayout"); aui.setAccessible(true); aui.setBoolean(n, true);
        Style action = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/list"));
        n.richDrawLines(List.of(FormattedCharSequence.composite(FormattedCharSequence.forward("Literal <b>", Style.EMPTY),
                FormattedCharSequence.forward(" /list", action))));
        var dom = new UiDomControlsTest.Dom(); dom.ids.put("overlay", dom.new Node("div"));
        var p = new NotificationHudPresentation((UiDomAccess) dom.access, java.util.function.Function.identity());
        HudDrawList frame = new HudDrawList();
        var bg = xin.vanilla.banira.common.data.NotificationData.class.getDeclaredField("bgColor"); bg.setAccessible(true);
        bg.set(n, xin.vanilla.banira.common.data.Color.argb(0xffffffff));
        frame.notification(n, new ScreenCoordinate(0, 0, 120, 42)); p.update(frame);
        assertTrue(dom.created.stream().anyMatch(node -> node.text.equals("Literal <b>")));
        assertTrue(dom.created.stream().allMatch(node -> node.events.isEmpty()));
        var spans = dom.created.stream().filter(node -> "notice-text".equals(node.attributes.get("class"))).toList();
        assertEquals(2, spans.size());
        dom.created.stream().filter(node -> "notice".equals(node.attributes.get("class"))
                || "notice normal".equals(node.attributes.get("class"))).forEach(node -> node.rectangle = new ScreenCoordinate(0, 0, 120, 42));
        dom.created.stream().filter(node -> "notice-body".equals(node.attributes.get("class"))).forEach(node -> node.rectangle = new ScreenCoordinate(6, 18, 108, 18));
        spans.get(0).rectangle = new ScreenCoordinate(6, 18, 20, 10);
        spans.get(1).rectangle = new ScreenCoordinate(30, 18, 35, 10);
        p.afterLayout(); assertSame(action, n.styleAtTextPoint(35, 22)); assertNull(n.styleAtTextPoint(70, 22));
        int created = dom.created.size(), writes = dom.created.stream().mapToInt(node -> node.writes).sum();
        p.update(frame); assertEquals(created, dom.created.size()); assertEquals(writes, dom.created.stream().mapToInt(node -> node.writes).sum());
        p.update(new HudDrawList()); assertTrue(dom.created.stream().allMatch(node -> node.removed)); p.close();
    }

    @Test public void overlayAssetsStayLightLocalAndDoNotChangeNativeUnreadAppearance() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/hud/overlay.html")) {
            assertNotNull(in); byte[] data = in.readAllBytes(); assertTrue(data.length < 1024);
            String html = new String(data, StandardCharsets.UTF_8);
            assertFalse(html.contains("<script")); assertFalse(html.contains("http"));
        }
        for (String name : List.of("mail", "normal", "success", "warning", "error")) {
            try (var in = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/icons/hud-" + name + ".png")) {
                assertNotNull(name, in); assertTrue(in.readAllBytes().length < 4096);
            }
        }
        assertEquals(0, xin.vanilla.banira.client.notification.NotificationHudAppearance.BACKGROUND_ALPHA);
    }

    @Test public void fixedAuiBodyGeometryDoesNotClipLowPaddingOrMultilineNotifications() {
        for (int legacyPadding : new int[]{0, 2, 5, 12}) {
            for (int lines = 1; lines <= 8; lines++) {
                int height = xin.vanilla.banira.client.notification.NotificationBubbleLayout.height(lines);
                assertEquals(lines * 10, height - 18 - 4);
                assertEquals(lines, xin.vanilla.banira.client.notification.NotificationBubbleLayout.lineLimit(height));
            }
        }
    }

    @Test public void unsupportedVanillaTextFormattingSelectsPerNotificationFallback() {
        for (Style style : List.of(Style.EMPTY.withObfuscated(true), Style.EMPTY.withStrikethrough(true),
                Style.EMPTY.withFont(new net.minecraft.resources.ResourceLocation("test", "font")))) {
            assertFalse(xin.vanilla.banira.client.notification.NotificationBubbleLayout.supports(List.of(FormattedCharSequence.forward("action", style))));
        }
        assertTrue(xin.vanilla.banira.client.notification.NotificationBubbleLayout.supports(List.of(
                FormattedCharSequence.forward("action", Style.EMPTY.withBold(true).withItalic(true).withUnderlined(true)))));
    }

    @Test public void disconnectedMissingHiddenAndIsolatedHostsDoNotRequestAuiGeometry() {
        var backend = new UiBackendController(() -> true, () -> true);
        var host = new AuiHudHost(backend, spec -> new NativeUiView(null));
        host.register(new xin.vanilla.banira.api.client.ui.UiHudSpec("test:hud", "banira_codex/hud/overlay.html", new xin.vanilla.banira.api.client.ui.UiHudBinding() {
            public boolean visible() { return true; }
            public void bind(xin.vanilla.banira.api.client.ui.UiView view) { }
            public void detach(xin.vanilla.banira.api.client.ui.UiDetachReason reason) { }
        }));
        assertFalse(host.canRender("test:hud")); host.tick(true);
        assertTrue(host.canRender("test:hud")); assertFalse(host.canRender("test:missing"));
        backend.isolate("test:hud"); assertFalse(host.canRender("test:hud"));
    }
}
