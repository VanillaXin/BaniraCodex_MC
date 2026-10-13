package xin.vanilla.banira.client.minimap;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import xin.vanilla.banira.internal.fabric.mixin.FabricMixinConfigPlugin;

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
        assertSharedRows("xin/vanilla/banira/internal/fabric/compat/minimap/JourneyMapNotificationPlugin");
    }

    @Test
    public void journeyNativeBatchWidthIsDescriptorGuarded() throws Exception {
        node("xin/vanilla/banira/internal/mixin/compat/minimap/JourneyMapTextBoundsMixin");
        ClassNode target = new ClassNode();
        String graphics = "Lnet/minecraft/client/gui/GuiGraphics;", component = "Lnet/minecraft/network/chat/Component;",
                buffers = "Lnet/minecraft/client/renderer/MultiBufferSource;";
        target.methods.add(new MethodNode(0, "drawBatchLabel", "(" + graphics + component
                + "Ljourneymap/client/render/draw/DrawStep$Pass;" + buffers
                + "DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V", null, null));
        Method match = FabricMixinConfigPlugin.class.getDeclaredMethod("matchesJourneyBounds",
                ClassNode.class, String.class, String.class, String.class);
        match.setAccessible(true);
        assertEquals(true, match.invoke(null, target, graphics, component, buffers));
        target.methods.clear();
        assertEquals(false, match.invoke(null, target, graphics, component, buffers));
    }

    @Test
    public void journeyRefreshTracksAutomaticOwnerExpiry() throws Exception {
        assertTrue(calls(node("xin/vanilla/banira/internal/fabric/compat/minimap/JourneyMapNotificationPlugin"),
                "xin/vanilla/banira/internal/client/minimap/MinimapLineRegistry", "selectedHost"));
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
    public void journeySupplierRespectsNativeScreenPolicyBeforeClaimingHost() throws Exception {
        assertTrue(calls(node("xin/vanilla/banira/internal/fabric/compat/minimap/JourneyMapNotificationPlugin"),
                "xin/vanilla/banira/internal/client/minimap/MinimapLineLayout", "allowsLegacyJourneyScreen"));
    }

    @Test
    public void journeyRefreshTracksSharedLayoutRevision() throws Exception {
        assertTrue(calls(node("xin/vanilla/banira/internal/fabric/compat/minimap/JourneyMapNotificationPlugin"),
                "xin/vanilla/banira/internal/client/minimap/MinimapLineRegistry", "layoutRevision"));
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
        Method match = FabricMixinConfigPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true);
        String mixin = "xin.vanilla.banira.internal.mixin.compat.minimap.XaeroInfoDisplayManagerMixin";
        assertEquals(true, match.invoke(null, mixin, manager));
        manager.methods.remove(0);
        assertEquals(false, match.invoke(null, mixin, manager));
    }
}
