package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import static org.junit.Assert.*;

public class UiPageReinitializationTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ui/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test public void repeatedInitializationRequestsSafeReloadBeforeParentInitialization() throws Exception {
        MethodNode init = source("AuiPageHost").methods.stream().filter(method -> method.name.equals("init")).findFirst().orElseThrow();
        boolean attached = false, reload = false, returned = false, parent = false;
        for (AbstractInsnNode instruction : init.instructions) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.endsWith("AuiPageHost$DocumentView") && call.name.equals("attached")) attached = true;
                if (attached && call.owner.endsWith("UiSessionHost") && call.name.equals("reload")) reload = true;
                if (call.owner.equals("com/sighs/apricityui/screen/ApricityScreen") && call.name.equals("init")) {
                    assertTrue("An already bound page must request reload before the parent can attach again", attached && reload && returned);
                    parent = true;
                }
            }
            if (reload && instruction.getOpcode() == Opcodes.RETURN) returned = true;
        }
        assertTrue("Parent initialization must remain available for the first attachment", parent);
    }

    @Test public void lowLevelAttachmentStillRejectsDoubleOwnership() throws Exception {
        assertTrue(source("UiDomView").methods.stream().filter(method -> method.name.equals("attach"))
                .flatMap(method -> java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false))
                .anyMatch(instruction -> instruction instanceof LdcInsnNode
                        && "Document already attached".equals(((LdcInsnNode) instruction).cst)));
    }

    @Test public void explicitResizeKeepsTheSessionHandoff() throws Exception {
        assertTrue(source("AuiPageHost").methods.stream().filter(method -> method.name.equals("resize"))
                .flatMap(method -> java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.endsWith("UiSessionHost")
                        && ((MethodInsnNode) instruction).name.equals("resize")));
    }
}
