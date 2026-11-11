package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import static org.junit.Assert.*;

public class UiLegacyContainerRendererTest {
    @Test
    public void containerBackgroundUsesTheNativeSafeFlatLayer() throws Exception {
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getClassLoader().getResourceAsStream("xin/vanilla/banira/internal/client/ui/AuiContainerDocument.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        boolean negative = false;
        int flushes = 0;
        for (var method : node.methods)
            if (method.name.equals("render")) for (var instruction : method.instructions) {
                if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode constant && Double.valueOf(-10).equals(constant.cst))
                    negative = true;
                if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call && call.name.equals("flush"))
                    flushes++;
            }
        assertFalse("A negative document layer can be occluded by the screen dim background", negative);
        assertEquals("Native and AUI batches must be ordered around container painting", 2, flushes);
        assertEquals(100, net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.SLOT_ITEM_BLIT_OFFSET);
        try (var jar = new java.util.jar.JarFile(System.getProperty("banira.aui.contractJar"));
             var stream = jar.getInputStream(jar.getJarEntry("com/sighs/apricityui/render/Base.class"))) {
            org.objectweb.asm.tree.ClassNode engine = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(stream).accept(engine, 0);
            var method = engine.methods.stream().filter(m -> m.name.equals("resolveFlatDocumentBaseZ")).findFirst().orElseThrow();
            var last = method.instructions.getLast();
            while (last.getOpcode() < 0) last = last.getPrevious();
            assertEquals(org.objectweb.asm.Opcodes.FRETURN, last.getOpcode());
            assertEquals(org.objectweb.asm.Opcodes.FCONST_1, last.getPrevious().getOpcode());
        }
    }

    @Test
    public void flushUsesTheRealMinecraftBufferAndClippingRestoresTheParent() throws Exception {
        var node = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/LegacyUiGraphics.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        var flush = node.methods.stream().filter(m -> m.name.equals("flush")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(flush.instructions.toArray()).anyMatch(i ->
                i instanceof org.objectweb.asm.tree.MethodInsnNode c
                        && c.owner.equals("net/minecraft/client/renderer/MultiBufferSource$BufferSource")
                        && c.name.equals("endBatch") && c.desc.equals("()V")));
        for (String operation : java.util.List.of("pushScissor", "popScissor")) {
            var body = node.methods.stream().filter(m -> m.name.equals(operation)).findFirst().orElseThrow();
            var calls = java.util.Arrays.stream(body.instructions.toArray())
                    .filter(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode)
                    .map(i -> (org.objectweb.asm.tree.MethodInsnNode) i).toList();
            assertEquals("Flush before changing the native clipping state", "flush", calls.get(0).name);
            assertTrue(calls.stream().anyMatch(c -> c.owner.equals("xin/vanilla/banira/client/util/AbstractGuiUtils")
                    && c.name.equals(operation)));
        }
    }

    @Test
    public void gameTextUsesTheLegacySeeThroughOverload() throws Exception {
        var node = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiGameElements.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
        }
        var body = node.methods.stream().filter(m -> m.name.equals("renderText")).findFirst().orElseThrow();
        var calls = java.util.Arrays.stream(body.instructions.toArray())
                .filter(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode)
                .map(i -> (org.objectweb.asm.tree.MethodInsnNode) i).toList();
        var draw = calls.stream().filter(c -> c.owner.equals("net/minecraft/client/gui/Font")
                && c.name.equals("drawInBatch")).findFirst().orElseThrow();
        assertEquals("(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I", draw.desc);
    }
}
