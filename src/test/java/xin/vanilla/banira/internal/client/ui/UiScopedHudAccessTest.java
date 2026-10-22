package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import static org.junit.Assert.*;

public class UiScopedHudAccessTest {
    private ClassNode source() throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input); new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test public void closingOneBranchDoesNotHideAnotherOwnersTooltip() throws Exception {
        MethodNode close = source().methods.stream().filter(method -> method.name.equals("close")).findFirst().orElseThrow();
        int hide = -1;
        for (int i = 0; i < close.instructions.size(); i++) {
            var instruction = close.instructions.get(i);
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/Tooltip") && call.name.equals("hide")) hide = i;
        }
        assertTrue("Exclusive document cleanup must remain", hide >= 0);
        boolean scoped = false;
        for (int i = 0; i < hide; i++) {
            var instruction = close.instructions.get(i);
            if (instruction instanceof FieldInsnNode field && field.name.equals("scopeRoot")) {
                var next = instruction.getNext();
                while (next != null && next.getOpcode() < 0) next = next.getNext();
                if (next instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.IFNONNULL
                        && close.instructions.indexOf(jump.label) > hide) scoped = true;
            }
        }
        assertTrue("Scoped cleanup must skip document-wide tooltip disposal", scoped);
    }

    @Test public void scopedLookupChecksItsOwnedNodeSet() throws Exception {
        MethodNode find = source().methods.stream().filter(method -> method.name.equals("find")).findFirst().orElseThrow();
        boolean root = false, membership = false;
        for (var instruction : find.instructions) {
            if (instruction instanceof FieldInsnNode field && field.name.equals("scopeRoot")) root = true;
            if (instruction instanceof MethodInsnNode call && call.name.equals("containsKey")) membership = true;
        }
        assertTrue(root && membership);
    }

    @Test public void oldOwnerConstructorAndScopedConstructorBothExist() throws Exception {
        var constructors = source().methods.stream().filter(method -> method.name.equals("<init>")).map(method -> method.desc).toList();
        assertTrue(constructors.contains("(Lcom/sighs/apricityui/init/Document;Ljava/util/function/BooleanSupplier;)V"));
        assertTrue(constructors.contains("(Lcom/sighs/apricityui/init/Document;Ljava/util/function/BooleanSupplier;Lcom/sighs/apricityui/init/Element;)V"));
    }
}
