package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.internal.client.ui.page.NotificationUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class UiNotificationTypesPresentationTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private NotificationUiSession session(UiNotificationPagesTest.Fixture fixture) throws Exception {
        return (NotificationUiSession) fixture.session("SettingsPort");
    }

    @Test
    public void templateReusesCommonConfigurationStylesAndHeader() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/notification-types.html")) {
            assertNotNull(stream);
            String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (String expected : new String[]{"config.css", "player-config-editor.css", "config-body", "player-config-page",
                    "id=\"brand\"", "id=\"module_name\"", "id=\"categories\"", "id=\"status\"", "id=\"error\"", "id=\"cancel\""})
                assertTrue(expected, html.contains(expected));
            assertFalse(html.contains("<script"));
        }
    }

    @Test
    public void compactFieldsKeepRawIdentityAndMillisecondUnits() throws Exception {
        var s = session(new UiNotificationPagesTest.Fixture());
        var root = new NativeUiView(new Object());
        s.bind(root);
        assertTrue(root.rowProviders.get("fields") instanceof UiRowPresentation);
        assertEquals(24, ((UiRowPresentation) root.rowProviders.get("fields")).rowHeight());
        var row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, 1);
        assertFalse(row.texts.get("key").toString().contains("test.first"));
        assertTrue(row.tooltips.get("key").toString().contains("test.first"));
        assertEquals("ms", row.texts.get("field_unit").toString());
        assertFalse(row.texts.get("label").toString().contains("test.first"));
        assertNotNull(row.tooltips.get("input"));
        assertEquals("0", row.drafts.get("input"));
    }

    @Test
    public void sidebarNavigatesWithoutFilteringOrLosingDrafts() throws Exception {
        var s = session(new UiNotificationPagesTest.Fixture());
        var root = new NativeUiView(new Object());
        s.bind(root);
        s.duration("test.first", "-1");
        assertTrue(root.rowProviders.get("categories") instanceof UiRowPresentation);
        var provider = (UiRowPresentation) root.rowProviders.get("categories");
        assertEquals("category_content", provider.scrollerId());
        assertFalse(provider.tracksPageScroll());
        assertEquals(20, provider.rowHeight());
        int target = -1;
        for (int i = 0; i < provider.size(); i++) if (provider.key(i).equals("type:test.second")) target = i;
        assertTrue(target >= 0);
        var row = new NativeUiView(new Object());
        provider.bind(row, target);
        row.activate("open");
        assertEquals(5, root.state().rowPosition(), 0);
        assertEquals("type:test.second:0", root.state().scrollAnchor());
        assertEquals(10, root.rowProviders.get("fields").size());
        assertEquals("-1", s.durationDraft("test.first"));
    }

    @Test
    public void filteredInvalidDraftStillAppearsInGlobalStatusAndBlocksSave() throws Exception {
        var f = new UiNotificationPagesTest.Fixture();
        var s = session(f);
        var root = new NativeUiView(new Object());
        s.bind(root);
        var row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, 1);
        row.inputDraft("input", "-1");
        s.query("test.second");
        assertEquals(5, root.rowProviders.get("fields").size());
        assertEquals(Boolean.TRUE, root.enabled.get("error"));
        assertTrue(root.texts.get("error").toString().contains("test.first"));
        assertFalse(s.save());
        assertEquals(0, f.saves);
        s.query("");
        root.rowProviders.get("fields").bind(row, 1);
        row.inputDraft("input", Long.toString(Long.MAX_VALUE));
        assertEquals(Boolean.FALSE, root.enabled.get("error"));
        assertTrue(s.save());
        assertEquals(1, f.saves);
        assertEquals(0, s.changedCount());
        assertEquals(Long.MAX_VALUE, f.settings.get("test.first").durationMs());
    }

    @Test
    public void retiredRowsAndNavigationCannotMutateReplacementView() throws Exception {
        var s = session(new UiNotificationPagesTest.Fixture());
        var first = new NativeUiView(new Object());
        s.bind(first);
        var field = new NativeUiView(new Object());
        first.rowProviders.get("fields").bind(field, 0);
        var nav = new NativeUiView(new Object());
        var provider = first.rowProviders.get("categories");
        assertNotNull(provider);
        provider.bind(nav, provider.size() - 1);
        var second = new NativeUiView(new Object());
        s.bind(second);
        field.input("input", UiValue.bool(true));
        nav.activate("open");
        assertFalse(s.settings("test.first").hidden());
        assertEquals(0, second.state().rowPosition(), 0);
        second.state().rowPosition(8);
        second.state().scrollAnchor("old");
        s.query("second");
        assertEquals(0, second.state().rowPosition(), 0);
        assertEquals("", second.state().scrollAnchor());
    }

    @Test
    public void searchUsesRegisteredModuleNameWithoutReplacingIds() throws Exception {
        NotificationTypeRegistry.registerModDisplayNameInternal("test", BaniraComponent.get().literal("Registry Module"));
        try {
            var s = session(new UiNotificationPagesTest.Fixture());
            var root = new NativeUiView(new Object());
            s.bind(root);
            s.query("registry module");
            assertEquals(10, root.rowProviders.get("fields").size());
            assertEquals("type:test.first:0", root.rowProviders.get("fields").key(0));
        } finally {
            NotificationTypeRegistry.registerModDisplayNameInternal("test", null);
        }
    }

    @Test
    public void visibleSubtitleUsesRegisteredDescriptionNotInternalId() throws Exception {
        NotificationTypeRegistry.registerInternal("test.first", BaniraComponent.get().literal("Teleport search progress"));
        try {
            var s = session(new UiNotificationPagesTest.Fixture());
            var root = new NativeUiView(new Object());
            s.bind(root);
            var row = new NativeUiView(new Object());
            root.rowProviders.get("fields").bind(row, 0);
            assertEquals("Teleport search progress", row.texts.get("key").toString());
            assertTrue(row.tooltips.get("key").toString().contains("test.first"));
            root.inputDraft("search", "test.first");
            assertEquals("test.first", s.query());
            assertEquals(5, root.rowProviders.get("fields").size());
        } finally {
            NotificationTypeRegistry.registerInternal("test.first", (xin.vanilla.banira.common.data.Component) null);
        }
    }
}
