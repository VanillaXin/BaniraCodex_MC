package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class AuiInitialPresentationTest {

    @Test
    public void sharedPageHeightHasAViewportAnchorWithoutPointerInput() throws Exception {
        try (lombok.var input = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/shared.css")) {
            assertNotNull(input);
            String css = new String(com.google.common.io.ByteStreams.toByteArray(input), StandardCharsets.UTF_8);
            assertTrue("Shared root must not depend on an unresolved percentage height", Pattern.compile("(?:html,body|body)\\s*\\{[^}]*height:100vh(?:;|})").matcher(css).find());
        }
    }

    @Test
    public void everyRetainedDrawingChecksVisibilityAndModalOwnership() throws Exception {
        ClassNode type = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        lombok.var render = type.methods.stream().filter(method -> method.name.equals("render")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        int guards = 0;
        for (lombok.var instruction : render.instructions) if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("paintable"))
            guards++;
        assertEquals("Rich text, option icons and game sprites must all respect the modal", 3, guards);
        lombok.var guard = type.methods.stream().filter(method -> method.name.equals("paintable")).findFirst();
        assertTrue("Retained drawing visibility guard required", guard.isPresent());
    }
}
