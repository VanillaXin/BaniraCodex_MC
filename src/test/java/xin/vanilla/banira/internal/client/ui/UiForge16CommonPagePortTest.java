package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.internal.client.ui.page.NavigationUiSession;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class UiForge16CommonPagePortTest {

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private ClassNode code(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void allFourNativePagesExposeSharedSessions() throws Exception {
        for (String name : new String[] { "CodexNavigationScreen", "InputFormScreen", "ConfirmDialogScreen", "ReadOnlyTextScreen" }) {
            ClassNode screen = code("client/gui/" + name);
            assertTrue(name, screen.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
            assertTrue(name, screen.methods.stream().anyMatch(method -> method.name.equals("uiPage")));
            assertTrue(name, screen.methods.stream().anyMatch(method -> method.name.equals("uiSession")));
        }
    }

    @Test
    public void navigationRetiresRowsWithoutChangingStableActions() {
        AtomicInteger opened = new AtomicInteger();
        NavigationUiSession session = new NavigationUiSession(BaniraComponent.get().literal("Navigation"), Collections.singletonList(new NavigationUiSession.Action("settings", BaniraComponent.get().literal("Settings"), opened::incrementAndGet, null)));
        NativeUiView root = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        session.bind(root);
        assertEquals(1, root.rowProviders.get("fields").size());
        assertEquals("settings", root.rowProviders.get("fields").key(0));
        root.rowProviders.get("fields").bind(row, 0);
        row.activate("open");
        assertEquals(1, opened.get());
        session.detach(UiDetachReason.RELOAD);
        row.activate("open");
        assertEquals(1, opened.get());
        assertEquals(0, row.subscriptionCount());
        session.bind(root);
        root.rowProviders.get("fields").bind(row, 0);
        row.activate("open");
        assertEquals(2, opened.get());
    }

    @Test
    public void formKeyChordsUseTheExistingForge16Parser() {
        assertEquals(java.util.Arrays.asList("left.control", "K"), xin.vanilla.banira.client.util.GLFWKeyUtils.parseDisplayString(" left.control + K "));
        assertTrue(xin.vanilla.banira.client.util.GLFWKeyUtils.parseDisplayString(" ").isEmpty());
    }

    @Test
    public void sharedPageWiringPreservesForge16EventOwners() throws Exception {
        ClassNode bootstrap = code("internal/client/BaniraCodexClientBootstrap");
        long wrappers = bootstrap.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray())).filter(instruction -> instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.startsWith("xin/vanilla/banira/client/event/BaniraClientEventHub$")).count();
        assertTrue(wrappers >= 5);
    }
}
