package xin.vanilla.banira.internal.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.internal.client.ui.page.NotificationUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric20NotificationPortTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private boolean calls(ClassNode node, String method, String owner, String name) {
        return node.methods.stream().filter(candidate -> method == null || candidate.name.equals(method))
                .flatMap(candidate -> Arrays.stream(candidate.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals("xin/vanilla/banira/" + owner) && call.name.equals(name));
    }

    @Test
    public void allThreeNativePagesUseTheirSharedSessionAndLifecycle() throws Exception {
        String[] screens = {"NotificationLogScreen", "NotificationTypeConfigScreen", "NotificationHudPositionScreen"};
        String[] factories = {"history", "types", "position"};
        for (int index = 0; index < screens.length; index++) {
            ClassNode page = node("client/gui/" + screens[index]);
            assertTrue(page.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
            assertTrue(calls(page, "<init>", "internal/client/ui/page/NotificationUiSession", factories[index]));
            assertTrue(calls(page, "<init>", "internal/client/ui/page/NotificationUiSession", "attachNative"));
            assertTrue(calls(page, "uiPage", "internal/client/ui/page/NotificationUiSession", "spec"));
            assertTrue(calls(page, "onClose", "internal/client/ui/CommonUiPages", "close"));
            assertTrue(calls(page, "onRemoved", "internal/client/ui/CommonUiPages", "removed"));
        }
    }

    @Test
    public void nativeHistoryActionsUseTheSharedReadOwner() throws Exception {
        ClassNode page = node("client/gui/NotificationLogScreen");
        assertTrue(calls(page, "selectEntry", "internal/client/ui/page/NotificationUiSession", "select"));
        assertTrue(calls(page, null, "internal/client/ui/page/NotificationUiSession", "markAllRead"));
        assertFalse(calls(page, "selectEntry", "client/util/NotificationManager", "markRead"));
        assertTrue(calls(page, "onClose", "internal/client/ui/page/NotificationUiSession", "afterParentReturn"));
    }

    @Test
    public void positionBindingsRetainTheExistingNativePreviewRenderer() throws Exception {
        ClassNode session = node("internal/client/ui/page/NotificationUiSession");
        assertTrue(calls(session, "bindPosition", "api/client/ui/UiView", "onPointer"));
        assertTrue(calls(session, "updatePosition", "api/client/ui/UiView", "hudPosition"));
        assertTrue(calls(node("internal/compat/aui/AuiDocumentAccess"), null,
                "client/notification/NotificationUnreadHud", "draw"));
        assertFalse(calls(session, null, "internal/client/ui/UiSessionHost", "registerHud"));
    }

    @Test
    public void newNotificationLabelsExistInBothLanguages() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            try (var input = getClass().getResourceAsStream("/assets/banira_codex/lang/" + language + ".json")) {
                assertNotNull(language, input);
                JsonObject values = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : new String[]{"format.banira_codex.player_config_invalid_status",
                        "word.banira_codex.notification_log_back", "word.banira_codex.notification_log_no_results",
                        "word.banira_codex.notification_type_config_search_hint"}) {
                    assertTrue(key, values.has(key));
                    assertFalse(key, values.get(key).getAsString().isBlank());
                    assertNotEquals(key, values.get(key).getAsString());
                }
            }
        }
    }

    @Test
    public void emptySearchResultsDoNotReadOrDiscardHistory() throws Exception {
        var fixture = new UiNotificationPagesTest.Fixture();
        fixture.history.append(new NotificationLogEntry().id(17).notificationType("example.notice"), 100);
        NotificationUiSession session = (NotificationUiSession) fixture.session("HistoryPort");
        NativeUiView view = new NativeUiView(new Object());
        session.bind(view);
        session.query("not-present-in-any-record");
        assertTrue(view.enabled.get("empty"));
        assertEquals(0, view.rowProviders.get("fields").size());
        assertEquals(0, fixture.marks);
        session.query("   ");
        assertFalse(view.enabled.get("empty"));
        assertEquals(1, view.rowProviders.get("fields").size());
        assertEquals(1, fixture.history.snapshot().size());
        assertEquals(0, fixture.marks);
    }
}
