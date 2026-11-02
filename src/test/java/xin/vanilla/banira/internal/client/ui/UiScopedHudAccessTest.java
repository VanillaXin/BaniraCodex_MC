package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UiScopedHudAccessTest {

    private ClassNode source() throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void closingOneBranchDoesNotHideAnotherOwnersTooltip() throws Exception {
        MethodNode close = source().methods.stream().filter(method -> method.name.equals("close")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        int hide = -1;
        for (int i = 0; i < close.instructions.size(); i++) {
            lombok.var instruction = close.instructions.get(i);
            if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.endsWith("/Tooltip") && ((MethodInsnNode) instruction).name.equals("hide"))
                hide = i;
        }
        assertTrue("Exclusive document cleanup must remain", hide >= 0);
        boolean scoped = false;
        for (int i = 0; i < hide; i++) {
            lombok.var instruction = close.instructions.get(i);
            if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).name.equals("scopeRoot")) {
                lombok.var next = instruction.getNext();
                while (next != null && next.getOpcode() < 0) next = next.getNext();
                if (next instanceof JumpInsnNode && ((JumpInsnNode) next).getOpcode() == Opcodes.IFNONNULL && close.instructions.indexOf(((JumpInsnNode) next).label) > hide)
                    scoped = true;
            }
        }
        assertTrue("Scoped cleanup must skip document-wide tooltip disposal", scoped);
    }

    @Test
    public void scopedLookupChecksItsOwnedNodeSet() throws Exception {
        MethodNode find = source().methods.stream().filter(method -> method.name.equals("find")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        boolean root = false, membership = false;
        for (lombok.var instruction : find.instructions) {
            if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).name.equals("scopeRoot"))
                root = true;
            if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("containsKey"))
                membership = true;
        }
        assertTrue(root && membership);
    }

    @Test
    public void oldOwnerConstructorAndScopedConstructorBothExist() throws Exception {
        lombok.var constructors = source().methods.stream().filter(method -> method.name.equals("<init>")).map(method -> method.desc).collect(java.util.stream.Collectors.toList());
        assertTrue(constructors.contains("(Lcom/sighs/apricityui/init/Document;Ljava/util/function/BooleanSupplier;)V"));
        assertTrue(constructors.contains("(Lcom/sighs/apricityui/init/Document;Ljava/util/function/BooleanSupplier;Lcom/sighs/apricityui/init/Element;)V"));
    }
}
