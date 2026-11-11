package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UiFirstPaintPreparationTest {

    private static ClassNode code(String suffix) throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = UiFirstPaintPreparationTest.class.getResourceAsStream("/xin/vanilla/banira/internal/client/ui/AuiPageHost" + suffix + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void firstPaintSettlesRealizedRowsBeforeDrawingTheDocument() throws Exception {
        lombok.var host = code("");
        List<String> order = new ArrayList<>();
        for (lombok.var method : host.methods)
            if (method.name.equals("render"))
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode && (((MethodInsnNode) instruction).name.equals("prepareFrame") || ((MethodInsnNode) instruction).owner.equals("com/sighs/apricityui/screen/ApricityScreen")))
                        order.add(((MethodInsnNode) instruction).name);
        assertEquals(com.google.common.collect.ImmutableList.of("prepareFrame", "render"), order);
        lombok.var prepare = code("$DocumentView").methods.stream().filter(method -> method.name.equals("prepareFrame")).findFirst();
        assertTrue("First-frame layout preparation is missing", prepare.isPresent());
        List<String> passes = new ArrayList<>();
        for (lombok.var instruction : prepare.get().instructions)
            if (instruction instanceof MethodInsnNode && com.google.common.collect.ImmutableList.of("prepare", "layoutReady", "afterLayout", "commitLayout").contains(((MethodInsnNode) instruction).name))
                passes.add(((MethodInsnNode) instruction).name);
        assertEquals(com.google.common.collect.ImmutableList.of("prepare", "layoutReady", "afterLayout", "prepare", "commitLayout"), passes);
        assertEquals("Preparation must be able to defer an unfinished initial layout", "()Z", prepare.get().desc);
    }

    @Test
    public void aFrameStillPaintsTheVendorDocumentOnlyOnce() throws Exception {
        int paints = 0;
        for (lombok.var method : code("").methods)
            if (method.name.equals("render"))
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("com/sighs/apricityui/screen/ApricityScreen") && ((MethodInsnNode) instruction).name.equals("render"))
                        paints++;
        assertEquals(1, paints);
    }
}
