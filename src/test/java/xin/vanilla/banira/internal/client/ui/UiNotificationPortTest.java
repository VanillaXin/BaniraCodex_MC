package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import xin.vanilla.banira.internal.client.ui.page.NotificationUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.*;

public class UiNotificationPortTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private ClassNode node(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void nativeTypeScrollerPreservesBothMinecraft21Axes() throws Exception {
        var node = node("client/gui/NotificationTypeConfigScreen");
        var scroll = node.methods.stream().filter(m -> m.name.equals("mouseScrolled")).findFirst().orElseThrow();
        assertEquals("(DDDD)Z", scroll.desc);
        boolean delegates = false;
        for (var instruction : scroll.instructions) if (instruction instanceof MethodInsnNode call && call.name.equals("mouseScrolled")) {
            assertEquals("(DDDD)Z", call.desc);
            delegates = true;
        }
        assertTrue(delegates);
    }

    @Test
    public void chatDraftRestorationHasOneOwnerForNativeAndAuiReturn() throws Exception {
        var hud = node("client/notification/NotificationUnreadHud");
        var open = hud.methods.stream().filter(m -> m.name.equals("historyScreen")).findFirst().orElseThrow();
        int screens = 0;
        for (var instruction : open.instructions) if (instruction instanceof TypeInsnNode type) {
            assertFalse("Anonymous native-only return handlers must not duplicate the shared callback", type.desc.startsWith("xin/vanilla/banira/client/notification/NotificationUnreadHud$"));
            if (type.desc.equals("xin/vanilla/banira/client/gui/NotificationLogScreen")) screens++;
        }
        assertEquals(1, screens);
        var history = node("client/gui/NotificationLogScreen");
        int registrations = 0, nativeReturns = 0;
        for (var method : history.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/NotificationUiSession")) {
                if (method.name.equals("<init>") && call.name.equals("onParentReturn")) registrations++;
                if (method.name.equals("onClose") && call.name.equals("afterParentReturn")) nativeReturns++;
            }
        assertEquals(1, registrations);
        assertEquals(1, nativeReturns);
    }

    @Test
    public void connectingPagesDoesNotEnableTheUnportedHudRenderer() throws Exception {
        var hud = node("client/notification/NotificationUnreadHud");
        for (var method : hud.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call)
                assertFalse(call.owner.equals("xin/vanilla/banira/internal/client/ui/SharedHudHosts"));
        var bootstrap = node("internal/client/BaniraCodexClientBootstrap");
        for (var method : bootstrap.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call)
                assertFalse(call.owner.equals("xin/vanilla/banira/internal/client/ui/SharedHudHosts") && call.name.equals("register"));
    }

    @Test
    public void newSearchAndInvalidInputLabelsResolveInBothLanguages() throws Exception {
        var fixture = new UiNotificationPagesTest.Fixture();
        var history = (NotificationUiSession) fixture.session("HistoryPort");
        var historyView = new NativeUiView(new Object());
        history.bind(historyView);
        var types = (NotificationUiSession) fixture.session("SettingsPort");
        var typeView = new NativeUiView(new Object());
        types.bind(typeView);
        types.duration("test.first", "-");
        for (String language : new String[]{"en_us", "zh_cn"}) {
            for (var label : new xin.vanilla.banira.common.data.Component[]{historyView.tooltips.get("back"), typeView.texts.get("search_hint"), typeView.texts.get("status")}) {
                assertNotNull(label);
                String text = label.getString(language);
                assertFalse(text.isBlank());
                assertFalse(text, text.contains("banira_codex.notification") || text.contains("banira_codex.player_config"));
            }
        }
        assertEquals(0, fixture.saves);
        assertEquals(0, fixture.marks);
    }
}
