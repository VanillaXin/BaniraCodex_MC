package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class AuiInitialPresentationTest {

    private ClassNode bytecode(String type) throws Exception {
        ClassNode result = new ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + type + ".class")) {
            assertNotNull(type, input);
            new ClassReader(input).accept(result, 0);
        }
        return result;
    }

    @Test
    public void initialRowsJoinCommittedLayoutBeforeTheFirstPaint() throws Exception {
        ClassNode host = bytecode("internal/client/ui/AuiPageHost");
        org.objectweb.asm.tree.MethodNode render = host.methods.stream().filter(method -> method.name.equals("render"))
                .findFirst().orElseThrow(AssertionError::new);
        List<String> draws = new ArrayList<>();
        for (org.objectweb.asm.tree.AbstractInsnNode instruction : render.instructions)
            if (instruction instanceof MethodInsnNode) draws.add(((MethodInsnNode) instruction).name);
        assertTrue(draws.indexOf("prepareFrame") >= 0);
        assertTrue(draws.indexOf("prepareFrame") < draws.indexOf("render"));
        ClassNode view = bytecode("internal/client/ui/AuiPageHost$DocumentView");
        org.objectweb.asm.tree.MethodNode prepare = view.methods.stream().filter(method -> method.name.equals("prepareFrame"))
                .findFirst().orElseThrow(AssertionError::new);
        List<String> sequence = new ArrayList<>();
        for (org.objectweb.asm.tree.AbstractInsnNode instruction : prepare.instructions)
            if (instruction instanceof MethodInsnNode) sequence.add(((MethodInsnNode) instruction).name);
        assertEquals(java.util.Arrays.asList("prepare", "layoutReady", "afterLayout", "prepare", "commitLayout"), sequence);
        assertTrue(view.fields.stream().anyMatch(field -> field.name.equals("firstLayoutPrepared") && field.desc.equals("Z")));
    }

    @Test
    public void layoutReadinessAndCommitUseTheActualDocumentLayout() throws Exception {
        ClassNode access = bytecode("internal/compat/aui/AuiDocumentAccess");
        for (String[] expected : new String[][]{{"layoutReady", "prepareRender"}, {"commitLayout", "commit"}}) {
            org.objectweb.asm.tree.MethodNode method = access.methods.stream().filter(value -> value.name.equals(expected[0]))
                    .findFirst().orElseThrow(AssertionError::new);
            assertTrue(java.util.Arrays.stream(method.instructions.toArray()).anyMatch(instruction ->
                    instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals(expected[1])
                            && ((MethodInsnNode) instruction).owner.startsWith("com/sighs/apricityui/")));
            assertTrue(java.util.Arrays.stream(method.instructions.toArray()).anyMatch(instruction ->
                    instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("current")));
        }
    }

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
        for (lombok.var instruction : render.instructions)
            if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("paintable"))
                guards++;
        assertEquals("Rich text, option icons and game sprites must all respect the modal", 3, guards);
        lombok.var guard = type.methods.stream().filter(method -> method.name.equals("paintable")).findFirst();
        assertTrue("Retained drawing visibility guard required", guard.isPresent());
    }
}
