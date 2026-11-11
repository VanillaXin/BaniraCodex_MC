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
    public void pageExtractionSeparatesHtmlAndNativeStatesWithoutMutatingGlobalTint() throws Exception {
        ClassNode type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ui/AuiPageHost.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        var render = type.methods.stream().filter(method -> method.name.equals("extractRenderState")).findFirst().orElseThrow();
        List<MethodInsnNode> calls = new ArrayList<>();
        for (var instruction : render.instructions)
            if (instruction instanceof MethodInsnNode call) calls.add(call);
        int html = -1, prepare = -1, retained = -1;
        List<Integer> strata = new ArrayList<>();
        for (int i = 0; i < calls.size(); i++) {
            var call = calls.get(i);
            assertFalse("Extraction must not read or change mutable global shader tint",
                    call.owner.equals("com/mojang/blaze3d/systems/RenderSystem"));
            if (call.owner.equals("com/sighs/apricityui/screen/ApricityScreen")
                    && call.name.equals("extractRenderState")) html = i;
            if (call.owner.endsWith("/AuiDocumentAccess") && call.name.equals("prepare")) prepare = i;
            if (call.owner.endsWith("/AuiDocumentAccess") && call.name.equals("render")) retained = i;
            if (call.name.equals("nextStratum")) strata.add(i);
        }
        final int htmlAt = html, retainedAt = retained;
        assertTrue("Prepare layout before HTML extraction", prepare >= 0 && html > prepare);
        assertTrue("Retained sprites follow the HTML submission", retained > html);
        assertTrue("Separate HTML from the caller's extracted state", strata.stream().anyMatch(i -> i < htmlAt));
        assertTrue("Separate native sprites from HTML", strata.stream().anyMatch(i -> i > htmlAt && i < retainedAt));
        assertEquals("Normal and exceptional exits isolate subsequent drawing", 2,
                strata.stream().filter(i -> i > retainedAt).count());
        assertFalse("Exceptional extraction paths retain cleanup and fallback", render.tryCatchBlocks.isEmpty());
    }
}
