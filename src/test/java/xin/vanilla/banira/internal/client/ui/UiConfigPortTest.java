package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.internal.client.ui.page.ConfigUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiConfigPortTest {
    @org.junit.Rule
    public final xin.vanilla.banira.test.NativeClientRule nativeClient = new xin.vanilla.banira.test.NativeClientRule();

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void nativeScrollRetainsBothMinecraft21Axes() throws Exception {
        var type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/client/gui/ConfigEditorScreen.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        var method = type.methods.stream().filter(m -> m.name.equals("mouseScrolled")).findFirst().orElseThrow();
        assertEquals("(DDDD)Z", method.desc);
        boolean delegates = false;
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode invocation
                    && invocation.name.equals("mouseScrolled")) {
                assertEquals("(DDDD)Z", invocation.desc);
                delegates = true;
            }
        assertTrue(delegates);
    }

    @Test
    public void unavailableEngineKeepsTheConfigurationSessionAndInvalidDraft() throws Exception {
        assertNull(getClass().getClassLoader().getResource("com/sighs/apricityui/screen/ApricityScreen.class"));
        var f = new UiConfigSessionTest.Fixture();
        f.field("number", ConfigValueType.INTEGER, 2);
        f.open();
        f.draft("number", "-");
        ConfigUiSession session = (ConfigUiSession) f.session;
        UiPageSpec page = session.spec();
        var backend = new UiBackendController(() -> true, () -> true);
        var owner = new AtomicReference<UiSessionHost>();
        var aui = new AuiUiBackend(owner::get);
        var displayed = new AtomicReference<Object>();
        var host = new UiSessionHost(backend, aui::create, displayed::set, value -> value instanceof net.minecraft.client.gui.screens.Screen);
        owner.set(host);
        assertTrue(host.open(page, null));
        assertTrue(displayed.get() instanceof ConfigEditorScreen);
        assertSame(session, ((ConfigEditorScreen) displayed.get()).uiSession());
        assertEquals("-", session.draft("number"));
        assertEquals(Integer.valueOf(2), f.holder.get("number"));
        assertFalse(backend.wantsAui(page.id()));
        assertTrue(f.sent.isEmpty());
        host.disconnect();
        assertFalse(session.save());
        assertFalse(session.sync(true));
        assertEquals(0, f.saves.get());
    }

    @Test
    public void newlyConnectedLabelsResolveInBothLanguages() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("value", ConfigValueType.STRING, "value");
        f.open();
        var view = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{xin.vanilla.banira.api.client.ui.UiView.class}, view);
        for (String lang : new String[]{"en_us", "zh_cn"}) {
            for (String id : new String[]{"search_hint", "category_title", "origin", "status", "fetch", "sync_all", "entry_count"}) {
                String label = view.texts.get(id).getString(lang);
                assertFalse(id + " in " + lang, label.isBlank());
                assertFalse(id + " in " + lang, label.contains("banira_codex.config"));
            }
        }
    }

    @Test
    public void snapshotDispatchReachesTheActiveSharedConfigurationSession() throws Exception {
        var type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraClientScreenService.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        for (String[] expected : new String[][]{{"applyRemoteConfigSnapshot", "acceptRemoteSnapshot"}, {"refreshOpenConfigEditor", "refresh"}}) {
            var method = type.methods.stream().filter(m -> m.name.equals(expected[0])).findFirst().orElseThrow();
            boolean delivered = false;
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode invocation
                        && invocation.owner.equals("xin/vanilla/banira/internal/client/ui/page/ConfigUiSession")
                        && invocation.name.equals(expected[1])) delivered = true;
            assertTrue(expected[0], delivered);
        }
    }
}
