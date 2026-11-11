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
        try (var input = UiFirstPaintPreparationTest.class.getResourceAsStream(
                "/xin/vanilla/banira/internal/client/ui/AuiPageHost" + suffix + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void firstPaintSettlesRealizedRowsBeforeDrawingTheDocument() throws Exception {
        var host = code("");
        List<String> order = new ArrayList<>();
        for (var method : host.methods)
            if (method.name.equals("render"))
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call
                            && (call.name.equals("prepareFrame") || call.owner.equals("com/sighs/apricityui/screen/ApricityScreen")))
                        order.add(call.name);
        assertEquals(List.of("prepareFrame", "render"), order);
        var prepare = code("$DocumentView").methods.stream().filter(method -> method.name.equals("prepareFrame")).findFirst();
        assertTrue("First-frame layout preparation is missing", prepare.isPresent());
        List<String> passes = new ArrayList<>();
        for (var instruction : prepare.get().instructions)
            if (instruction instanceof MethodInsnNode call
                    && List.of("prepare", "layoutReady", "afterLayout", "commitLayout").contains(call.name))
                passes.add(call.name);
        assertEquals(List.of("prepare", "layoutReady", "afterLayout", "prepare", "commitLayout"), passes);
        assertEquals("Preparation must be able to defer an unfinished initial layout", "()Z", prepare.get().desc);
    }

    @Test
    public void aFrameStillPaintsTheVendorDocumentOnlyOnce() throws Exception {
        int paints = 0;
        for (var method : code("").methods)
            if (method.name.equals("render"))
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call
                            && call.owner.equals("com/sighs/apricityui/screen/ApricityScreen") && call.name.equals("render"))
                        paints++;
        assertEquals(1, paints);
    }
}
