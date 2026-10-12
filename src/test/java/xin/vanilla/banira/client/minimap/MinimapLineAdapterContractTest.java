package xin.vanilla.banira.client.minimap;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
        assertSharedRows("xin/vanilla/banira/internal/compat/minimap/LegacyJourneyMapNotifications");
    }

    @Test
    public void journeyBoundsMixinIsPresentAndDescriptorGuarded() throws Exception {
        String name = "xin/vanilla/banira/internal/mixin/compat/minimap/JourneyMapTextBoundsMixin";
        node(name);
        ClassNode target = new ClassNode();
        String descriptor = "(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/renderer/MultiBufferSource;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;Ljava/lang/Integer;FIFDZD)V";
        target.methods.add(new MethodNode(0, "drawBatchLabel", descriptor, null, null));
        Method match = OptionalCompatibilityMixinPlugin.class.getDeclaredMethod("matchesMinimap", String.class, ClassNode.class);
        match.setAccessible(true);
        assertEquals(true, match.invoke(null, name.replace('/', '.'), target));
        target.methods.clear();
        assertEquals(false, match.invoke(null, name.replace('/', '.'), target));
    }

    @Test
    public void journeyBatchLabelsHaveTheSameWidthConstraint() throws Exception {
        assertTrue(node("xin/vanilla/banira/internal/mixin/compat/minimap/JourneyMapTextBoundsMixin")
                .methods.stream().anyMatch(method -> method.name.equals("banira$boundInformationBatch")));
    }

    private String annotationText(Object value) {
        if (value instanceof org.objectweb.asm.tree.AnnotationNode)
            return annotationText(((org.objectweb.asm.tree.AnnotationNode) value).values);
        if (value instanceof java.util.List<?>) {
            StringBuilder text = new StringBuilder();
            for (Object item : (java.util.List<?>) value) text.append(annotationText(item));
            return text.toString();
        }
        return String.valueOf(value);
    }

    @Test
    public void ftbTextAnchorUsesTheNativePoseStackDescriptor() throws Exception {
        ClassNode target = node("xin/vanilla/banira/internal/mixin/compat/minimap/FtbChunksNotificationMixin");
        boolean matched = false;
        for (MethodNode method : target.methods) {
            if (!method.name.equals("banira$placeNativeText")) continue;
            assertNotNull(method.visibleAnnotations);
            for (org.objectweb.asm.tree.AnnotationNode annotation : method.visibleAnnotations) {
                String values = annotationText(annotation.values);
                matched |= values.contains("Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V");
            }
        }
        assertTrue("Text anchor must target the native PoseStack method", matched);
    }

    @Test
    public void ftbNativeTextListConsumesIndependentInformationRows() throws Exception {
        assertSharedRows("xin/vanilla/banira/internal/mixin/compat/minimap/FtbChunksNotificationMixin");
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
