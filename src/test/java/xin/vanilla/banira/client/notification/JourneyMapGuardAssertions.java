package xin.vanilla.banira.client.notification;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.Assert.*;

final class JourneyMapGuardAssertions {
    private JourneyMapGuardAssertions() {
    }

    static void assertNativeGuard(ClassNode node, String owner, String method) {
        MethodNode allowed = node.methods.stream().filter(m -> m.name.equals("allowed")).findFirst().orElse(null);
        assertNotNull("Missing native map guard", allowed);
        boolean guarded = false;
        for (AbstractInsnNode instruction : allowed.instructions) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!call.owner.equals(owner) || !call.name.equals(method)) continue;
            AbstractInsnNode next = nextExecutable(instruction);
            assertTrue("Native enabled result must control a branch", next instanceof JumpInsnNode);
            assertEquals(Opcodes.IFEQ, next.getOpcode());
            assertEquals("Disabled branch must return false", Opcodes.ICONST_0,
                    nextExecutable(((JumpInsnNode) next).label).getOpcode());
            guarded = true;
        }
        assertTrue("Native guard not checked: " + owner + "." + method, guarded);
        assertSupplierCannotClaimBeforeAllowed(node);
    }

    private static AbstractInsnNode nextExecutable(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        assertNotNull(next);
        return next;
    }

    private static void assertSupplierCannotClaimBeforeAllowed(ClassNode node) {
        boolean found = false;
        for (MethodNode method : node.methods) {
            int claimIndex = -1;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.endsWith("/NotificationMinimapBridge") && call.name.equals("rows")) {
                    claimIndex = method.instructions.indexOf(instruction);
                    break;
                }
            }
            if (claimIndex < 0) continue;
            boolean branchBeforeClaim = false;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.owner.equals(node.name) || !call.name.equals("allowed")) continue;
                AbstractInsnNode next = nextExecutable(instruction);
                if (!(next instanceof JumpInsnNode) || next.getOpcode() != Opcodes.IFNE) continue;
                JumpInsnNode jump = (JumpInsnNode) next;
                int target = method.instructions.indexOf(jump.label);
                if (target > claimIndex) continue;
                boolean returnsBeforeTarget = false;
                for (AbstractInsnNode p = jump.getNext(); p != jump.label; p = p.getNext()) {
                    if (p.getOpcode() == Opcodes.ARETURN) returnsBeforeTarget = true;
                }
                branchBeforeClaim |= returnsBeforeTarget;
            }
            assertTrue("Hidden map must return before observing or claiming rows", branchBeforeClaim);
            found = true;
        }
        assertTrue("No native row supplier was verified", found);
    }
}
