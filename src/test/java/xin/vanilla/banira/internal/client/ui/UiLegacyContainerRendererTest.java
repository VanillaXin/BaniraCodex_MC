package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class UiLegacyContainerRendererTest {

    @Test
    public void containerBackgroundUsesTheNativeSafeFlatLayer() throws Exception {
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        try (lombok.var input = getClass().getClassLoader().getResourceAsStream("xin/vanilla/banira/internal/client/ui/AuiContainerDocument.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        boolean negative = false;
        int flushes = 0;
        for (lombok.var method : node.methods) if (method.name.equals("render"))
            for (lombok.var instruction : method.instructions) {
                if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode && Double.valueOf(-10).equals(((org.objectweb.asm.tree.LdcInsnNode) instruction).cst))
                    negative = true;
                if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode && ((org.objectweb.asm.tree.MethodInsnNode) instruction).name.equals("flush"))
                    flushes++;
            }
        assertFalse("A negative document layer can be occluded by the screen dim background", negative);
        assertEquals("Native and AUI batches must be ordered around container painting", 2, flushes);
        org.objectweb.asm.tree.ClassNode container = new org.objectweb.asm.tree.ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/net/minecraft/client/gui/screen/inventory/ContainerScreen.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(container, 0);
        }
        assertTrue(container.methods.stream().anyMatch(method -> java.util.Arrays.stream(method.instructions.toArray()).anyMatch(i ->
                i instanceof org.objectweb.asm.tree.LdcInsnNode && Float.valueOf(100).equals(((org.objectweb.asm.tree.LdcInsnNode) i).cst))
                && java.util.Arrays.stream(method.instructions.toArray()).anyMatch(i -> i instanceof org.objectweb.asm.tree.FieldInsnNode
                && ((org.objectweb.asm.tree.FieldInsnNode) i).name.equals("blitOffset"))));
        try (lombok.var jar = new java.util.jar.JarFile(System.getProperty("banira.aui.contractJar"));
            lombok.var stream = jar.getInputStream(jar.getJarEntry("com/sighs/apricityui/render/Base.class"))) {
            org.objectweb.asm.tree.ClassNode engine = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(engine, 0);
            lombok.var method = engine.methods.stream().filter(m -> m.name.equals("resolveFlatDocumentBaseZ")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
            lombok.var last = method.instructions.getLast();
            while (last.getOpcode() < 0) last = last.getPrevious();
            assertEquals(org.objectweb.asm.Opcodes.FRETURN, last.getOpcode());
            assertEquals(org.objectweb.asm.Opcodes.FCONST_1, last.getPrevious().getOpcode());
        }
    }

    @Test
    public void flushUsesTheRealMinecraftBufferAndClippingRestoresTheParent() throws Exception {
        lombok.var node = new org.objectweb.asm.tree.ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        lombok.var flush = node.methods.stream().filter(m -> m.name.equals("flush")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        assertTrue(java.util.Arrays.stream(flush.instructions.toArray()).anyMatch(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode && ((org.objectweb.asm.tree.MethodInsnNode) i).owner.equals("net/minecraft/client/renderer/IRenderTypeBuffer$Impl") && ((org.objectweb.asm.tree.MethodInsnNode) i).name.equals("endBatch") && ((org.objectweb.asm.tree.MethodInsnNode) i).desc.equals("()V")));
        for (String operation : com.google.common.collect.ImmutableList.of("pushScissor", "popScissor")) {
            lombok.var body = node.methods.stream().filter(m -> m.name.equals(operation)).findFirst().orElseThrow(java.util.NoSuchElementException::new);
            lombok.var calls = java.util.Arrays.stream(body.instructions.toArray()).filter(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode).map(i -> (org.objectweb.asm.tree.MethodInsnNode) i).collect(java.util.stream.Collectors.toList());
            assertEquals("Flush before changing the native clipping state", "flush", calls.get(0).name);
            assertTrue(calls.stream().anyMatch(c -> c.owner.equals("xin/vanilla/banira/client/util/AbstractGuiUtils") && c.name.equals(operation)));
        }
    }

    @Test
    public void gameTextUsesTheLegacySeeThroughOverload() throws Exception {
        lombok.var node = new org.objectweb.asm.tree.ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiGameElements.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        lombok.var body = node.methods.stream().filter(m -> m.name.equals("renderText")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        lombok.var calls = java.util.Arrays.stream(body.instructions.toArray()).filter(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode).map(i -> (org.objectweb.asm.tree.MethodInsnNode) i).collect(java.util.stream.Collectors.toList());
        lombok.var draw = calls.stream().filter(c -> c.owner.equals("net/minecraft/client/gui/FontRenderer") && c.name.equals("drawInBatch")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        assertEquals("(Lnet/minecraft/util/IReorderingProcessor;FFIZLnet/minecraft/util/math/vector/Matrix4f;Lnet/minecraft/client/renderer/IRenderTypeBuffer;ZII)I", draw.desc);
    }
}
