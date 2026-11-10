package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import static org.junit.Assert.*;

public class UiPointerEventDispatchTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void pointerOverlayHasItsOwnRaisedPoseAfterBufferedItems() throws Exception {
        MethodNode render = source("internal/client/ui/UiPagePointer").methods.stream()
                .filter(m -> m.name.equals("render")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        boolean flushed = false, pushed = false, raised = false, painted = false, popped = false;
        for (AbstractInsnNode instruction : render.instructions) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.name.equals("flush") && !pushed) flushed = true;
            if (call.name.equals("pushPose") || call.name.equals("m_85836_")) {
                assertTrue(flushed);
                pushed = true;
            }
            if (call.name.equals("translate") || call.name.equals("m_252880_")) {
                assertTrue(pushed);
                raised = true;
            }
            if (call.owner.endsWith("SharedHudHosts") && call.name.equals("render")) {
                assertTrue(raised);
                painted = true;
            }
            if (call.name.equals("popPose") || call.name.equals("m_85849_")) popped = true;
        }
        assertTrue(painted && popped);
        assertFalse(render.tryCatchBlocks.isEmpty());
    }

    @Test
    public void pointerHudUsesTheEngineForegroundPlaneAbovePageItems() throws Exception {
        ClassNode hud = source("internal/client/ui/AuiHudDocument");
        MethodNode render = hud.methods.stream().filter(m -> m.name.equals("render")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        boolean foreground = false, explicitPlane = false;
        for (AbstractInsnNode instruction : render.instructions) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.owner.endsWith("/Base") && call.name.equals("getFlatOverlayZ")) foreground = true;
            if (call.owner.endsWith("/Base") && call.name.equals("drawOverlayDocument") && call.desc.endsWith(";F)V")) {
                assertTrue(foreground);
                explicitPlane = true;
            }
        }
        assertTrue(explicitPlane);
        MethodNode constructor = hud.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        assertTrue(java.util.Arrays.stream(constructor.instructions.toArray()).anyMatch(instruction ->
                instruction instanceof LdcInsnNode && SharedHudHosts.POINTER.equals(((LdcInsnNode) instruction).cst)));
    }

}
