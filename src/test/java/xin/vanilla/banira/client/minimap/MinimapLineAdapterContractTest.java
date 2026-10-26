package xin.vanilla.banira.client.minimap;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LdcInsnNode;
import xin.vanilla.banira.internal.mixin.OptionalCompatibilityMixinPlugin;

import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class MinimapLineAdapterContractTest {
    private static final String BRIDGE = "xin/vanilla/banira/client/notification/NotificationMinimapBridge";

    private ClassNode node(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/" + name + ".class")) {
            assertNotNull("Missing minimap adapter: " + name, input);
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, 0);
            return result;
        }
    }

    private boolean calls(ClassNode node, String owner, String method) {
        for (MethodNode m : node.methods) for (AbstractInsnNode instruction : m.instructions) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals(owner) && call.name.equals(method)) return true;
            }
        }
        return false;
    }

    private void assertSharedRows(String name) throws Exception {
        assertTrue("Adapter is restricted to the unread notification: " + name, calls(node(name), BRIDGE, "rows"));
    }

    @Test
    public void xaeroCompilerConsumesIndependentInformationRows() throws Exception {
        assertSharedRows("xin/vanilla/banira/internal/compat/minimap/XaeroNotificationInfo");
    }

    @Test
    public void xaeroWrapsUnspacedTextAtNativeGlyphBoundaries() throws Exception {
        assertTrue(calls(node("xin/vanilla/banira/internal/compat/minimap/XaeroNotificationInfo"),
                "xin/vanilla/banira/internal/client/minimap/MinimapTextLayout", "wrap"));
    }

    @Test
    public void journeySupplierConsumesIndependentInformationRows() throws Exception {
        assertSharedRows("xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin");
    }

    @Test
    public void journeyNativeBatchWidthIsDescriptorGuarded() throws Exception {
        node("xin/vanilla/banira/internal/mixin/compat/minimap/JourneyMapTextBoundsMixin");
        ClassNode target = new ClassNode();
        target.methods.add(new MethodNode(0, "drawBatchLabel",
                "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;Ljourneymap/client/render/draw/DrawStep$Pass;Lnet/minecraft/client/renderer/MultiBufferSource;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V", null, null));
        Method match = OptionalCompatibilityMixinPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true);
        assertEquals(true, match.invoke(null, "JourneyMapTextBoundsMixin", target));
        target.methods.clear();
        assertEquals(false, match.invoke(null, "JourneyMapTextBoundsMixin", target));
    }

    @Test
    public void ftbNativeComponentsConsumeIndependentInformationRows() throws Exception {
        assertSharedRows("xin/vanilla/banira/internal/compat/minimap/FtbNotificationInfo");
    }

    @Test
    public void ftbLateRegistrationRefreshesNativeOrderWithoutForcingVisibility() throws Exception {
        ClassNode adapter = node("xin/vanilla/banira/internal/compat/minimap/FtbNotificationInfo");
        assertTrue(calls(adapter, "dev/ftb/mods/ftbchunks/client/FTBChunksClient", "setupComponents"));
        assertFalse(calls(adapter, "dev/ftb/mods/ftbchunks/api/client/FTBChunksClientAPI", "setMinimapComponentEnabled"));
    }

    @Test
    public void atlasUsesSharedRowsAndFullStackHeight() throws Exception {
        assertTrue(calls(node("xin/vanilla/banira/internal/mixin/compat/minimap/MapAtlasesNotificationMixin"),
                "xin/vanilla/banira/client/notification/NotificationMapLineRenderer", "drawRowsCentered"));
    }

    @Test
    public void journeyVisibleMapRowsRetainNativePermissions() throws Exception {
        ClassNode adapter = node("xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin");
        int checked = 0;
        for (MethodNode method : adapter.methods) {
            ClassNode boundary = new ClassNode();
            boundary.methods.add(method);
            if (!method.name.equals("refreshLayout") && !calls(boundary, BRIDGE, "rows")) continue;
            assertFalse(method.name + " suppresses visible map rows behind screens", calls(boundary,
                    "xin/vanilla/banira/internal/client/minimap/MinimapLineLayout", "allowsLegacyJourneyScreen"));
            assertTrue(method.name + " must retain the map switch", calls(boundary,
                    "journeymap/client/ui/UIManager", "isMiniMapEnabled"));
            assertTrue(method.name + " must retain native permissions", calls(boundary,
                    "journeymap/client/InternalStateHandler", "isMinimapEnabled"));
            checked++;
        }
        assertEquals("Supplier and layout refresh must both be checked", 2, checked);
    }

    @Test
    public void journeyRefreshTracksSharedLayoutRevision() throws Exception {
        assertTrue(calls(node("xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin"),
                "xin/vanilla/banira/internal/client/minimap/MinimapLineRegistry", "layoutRevision"));
    }

    @Test
    public void journeyNativeScreenPolicyMatchesNativeVisibility() throws Exception {
        Method policy;
        try {
            policy = Class.forName("xin.vanilla.banira.internal.client.minimap.MinimapLineLayout")
                    .getDeclaredMethod("allowsNativeJourneyScreen", boolean.class, boolean.class,
                            boolean.class, boolean.class, boolean.class);
        } catch (NoSuchMethodException missing) {
            fail("Missing native JourneyMap screen visibility policy");
            return;
        }
        for (int flags = 0; flags < 32; flags++) {
            boolean screen = (flags & 1) != 0;
            boolean chat = (flags & 2) != 0;
            boolean hideGui = (flags & 4) != 0;
            boolean behindScreens = (flags & 8) != 0;
            boolean nativeAllowed = (flags & 16) != 0;
            boolean expected = screen && !chat ? behindScreens && nativeAllowed : !hideGui;
            assertEquals("Native screen flags " + flags, Boolean.valueOf(expected),
                    policy.invoke(null, screen, chat, hideGui, behindScreens, nativeAllowed));
        }
    }

    @Test
    public void journeySupplierAndLayoutDoNotReactivateAnInvisibleNativeMap() throws Exception {
        String owner = "xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin";
        ClassNode adapter = node(owner);
        int checked = 0;
        for (MethodNode method : adapter.methods) {
            ClassNode boundary = new ClassNode();
            boundary.methods.add(method);
            if (!method.name.equals("refreshLayout") && !calls(boundary, BRIDGE, "rows")) continue;
            assertTrue(method.name + " can reactivate a hidden native map", calls(boundary, owner, "nativeScreenVisible"));
            checked++;
        }
        assertEquals(2, checked);
        assertTrue(calls(adapter, "journeymap/client/event/handlers/ScreenEventHandler", "getInstance"));
        assertTrue(calls(adapter, "java/lang/reflect/Method", "invoke"));
        boolean nativeRule = false;
        for (MethodNode method : adapter.methods) for (AbstractInsnNode instruction : method.instructions)
            if (instruction instanceof LdcInsnNode && "allowableDrawScreens".equals(((LdcInsnNode) instruction).cst))
                nativeRule = true;
        assertTrue("Native screen exclusions must be delegated", nativeRule);
    }

    @Test
    public void journeyVisibleSupplierReturnsTheNativeSnapshotWithoutAuiTagging() throws Exception {
        ClassNode adapter = node("xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin");
        int checked = 0;
        for (MethodNode method : adapter.methods) {
            ClassNode boundary = new ClassNode();
            boundary.methods.add(method);
            if (!calls(boundary, BRIDGE, "rows")) continue;
            assertFalse("Visible rows must not acquire an AUI identity", calls(boundary,
                    "xin/vanilla/banira/internal/client/ui/MinimapUiRows", "tag"));
            assertTrue("Return the registered row's native component", calls(boundary,
                    "xin/vanilla/banira/internal/client/minimap/MinimapLineSnapshot", "text"));
            checked++;
        }
        assertEquals(1, checked);
    }

    @Test
    public void notificationHostAndResetUseTheSameSharedRegistry() throws Exception {
        ClassNode bridge = node(BRIDGE);
        assertTrue(calls(bridge, "xin/vanilla/banira/internal/client/minimap/MinimapLineRegistry", "selectedHost"));
        assertTrue(calls(bridge, "xin/vanilla/banira/internal/client/minimap/MinimapLineRegistry", "resetHosts"));
    }

    @Test
    public void notificationSupplierDoesNotReenterRegistrySnapshot() throws Exception {
        MethodNode raw = null;
        for (MethodNode method : node(BRIDGE).methods) if (method.name.equals("notificationText")) raw = method;
        assertNotNull("Missing non-recursive notification supplier", raw);
        for (AbstractInsnNode instruction : raw.instructions) if (instruction instanceof MethodInsnNode) {
            MethodInsnNode call = (MethodInsnNode) instruction;
            assertFalse(call.name, call.owner.equals(BRIDGE) && (call.name.equals("rows")
                    || call.name.equals("text") || call.name.equals("claim")));
        }
    }

    @Test
    public void xaeroManagerRegistrationIsOptionalAndDescriptorGuarded() throws Exception {
        node("xin/vanilla/banira/internal/mixin/compat/minimap/XaeroInfoDisplayManagerMixin");
        ClassNode manager = new ClassNode();
        manager.methods.add(new MethodNode(0, "add", "(Lxaero/hud/minimap/info/InfoDisplay;)V", null, null));
        manager.methods.add(new MethodNode(0, "applyLocalConfig", "()V", null, null));
        Method match = OptionalCompatibilityMixinPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true);
        String mixin = "xin.vanilla.banira.internal.mixin.compat.minimap.XaeroInfoDisplayManagerMixin";
        assertEquals(true, match.invoke(null, mixin, manager));
        manager.methods.remove(0);
        assertEquals(false, match.invoke(null, mixin, manager));
    }
}
