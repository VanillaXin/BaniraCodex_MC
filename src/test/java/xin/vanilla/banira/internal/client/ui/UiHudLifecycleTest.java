package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

import static org.junit.Assert.*;

public class UiHudLifecycleTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var in = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    @Test
    public void documentOwnerCannotRecursivelyReenterDomCurrentCheck() throws Exception {
        ClassNode node = source("internal/client/ui/AuiHudDocument");
        for (MethodNode method : node.methods)
            if (method.name.startsWith("lambda$")) {
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        assertFalse("Document current predicate must not reenter UiDomView.live", call.name.equals("live"));
                    }
            }
        for (MethodNode method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof InvokeDynamicInsnNode) {
                    var dynamic = (InvokeDynamicInsnNode) instruction;
                    for (Object arg : dynamic.bsmArgs)
                        if (arg instanceof Handle) assertNotEquals("live", ((Handle) arg).getName());
                }
    }

    @Test
    public void persistentDocumentUsesInstanceDisposalAndNoMouseRelease() throws Exception {
        ClassNode node = source("internal/client/ui/AuiHudDocument");
        boolean manual = false, overlay = false;
        for (var method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    var call = (MethodInsnNode) instruction;
                    if (call.name.equals("setManuallyRendered")) manual = true;
                    if (call.name.equals("drawOverlayDocument")) overlay = true;
                    assertFalse(call.name.equals("releaseMouse") || call.name.equals("setScreen") || call.name.equals("drawPseudoCursor"));
                    if (call.name.equals("remove") && call.owner.endsWith("/Document"))
                        assertEquals("Instance ownership only", "()V", call.desc);
                }
        assertTrue(manual);
        assertTrue(overlay);
    }

    @Test
    public void nativePointerHasOneGraphicsAwarePersistentEntrypoint() {
        boolean found = Arrays.stream(xin.vanilla.banira.client.gui.widget.MouseWidget.class.getMethods()).anyMatch(m ->
                m.getName().equals("draw") && Arrays.equals(m.getParameterTypes(), new Class[]{net.minecraft.client.gui.GuiGraphics.class, int.class, int.class}));
        assertTrue("Pointer must share its original state with the persistent renderer", found);
    }

    @Test
    public void customRegisteredHudsAreIncludedInTheRenderPhase() {
        assertTrue("Public registrations must reach an actual render phase", Arrays.stream(AuiHudHost.class.getMethods()).anyMatch(m -> m.getName().equals("renderOthers")));
    }
}
