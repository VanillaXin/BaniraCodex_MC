package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class AuiInitialPresentationTest {
    @Test public void sharedPageHeightHasAViewportAnchorWithoutPointerInput() throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/shared.css")) {
            assertNotNull(input);
            String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue("Shared root must not depend on an unresolved percentage height",
                    Pattern.compile("(?:html,body|body)\\s*\\{[^}]*height:100vh(?:;|})").matcher(css).find());
        }
    }

    @Test public void everyRetainedDrawingChecksVisibilityAndModalOwnership() throws Exception {
        ClassNode type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input); new ClassReader(input).accept(type, 0);
        }
        var render = type.methods.stream().filter(method -> method.name.equals("render")).findFirst().orElseThrow();
        int guards = 0;
        for (var instruction : render.instructions) if (instruction instanceof MethodInsnNode call
                && call.name.equals("paintable")) guards++;
        assertEquals("Rich text, option icons and game sprites must all respect the modal", 3, guards);
        var guard = type.methods.stream().filter(method -> method.name.equals("paintable")).findFirst();
        assertTrue("Retained drawing visibility guard required", guard.isPresent());
    }
}
