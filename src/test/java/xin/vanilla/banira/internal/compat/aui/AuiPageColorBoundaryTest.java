package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class AuiPageColorBoundaryTest {
    @Test
    public void pagePassCopiesNeutralizesAndRestoresShaderColor() throws Exception {
        ClassNode type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ui/AuiPageHost.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        var render = type.methods.stream().filter(method -> method.name.equals("render")).findFirst().orElseThrow();
        List<MethodInsnNode> calls = new ArrayList<>();
        for (var instruction : render.instructions)
            if (instruction instanceof MethodInsnNode call) calls.add(call);
        int read = -1, clone = -1, neutral = -1, html = -1;
        List<Integer> restores = new ArrayList<>();
        for (int i = 0; i < calls.size(); i++) {
            var call = calls.get(i);
            if (call.name.equals("getShaderColor")) read = i;
            if (call.owner.equals("[F") && call.name.equals("clone")) clone = i;
            if (call.owner.equals("com/sighs/apricityui/screen/ApricityScreen") && call.name.equals("render")) html = i;
            if (call.name.equals("setShaderColor")) {
                if (neutral < 0) neutral = i;
                else restores.add(i);
            }
        }
        assertTrue("A mutable shader color array must be copied", read >= 0 && clone > read);
        assertTrue("Flush and neutralize before HTML rendering", clone < neutral && neutral > 0
                && calls.get(neutral - 1).name.equals("flush") && html > neutral);
        assertFalse("Every exit must restore the caller tint", restores.isEmpty());
        for (int restore : restores) assertTrue(restore > html);
        assertFalse("Exceptional drawing paths must retain cleanup", render.tryCatchBlocks.isEmpty());
    }
}
