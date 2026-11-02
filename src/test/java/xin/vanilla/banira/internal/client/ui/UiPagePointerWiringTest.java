package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static org.junit.Assert.*;

public class UiPagePointerWiringTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private boolean calls(ClassNode owner, String method, String target, String call) {
        return owner.methods.stream().filter(m -> m.name.equals(method))
                .flatMap(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false))
                .anyMatch(i -> i instanceof MethodInsnNode
                        && ((MethodInsnNode) i).owner.endsWith(target) && ((MethodInsnNode) i).name.equals(call));
    }

    @Test
    public void ownedPagesDrawAndForwardAllPointerFeedback() throws Exception {
        ClassNode host = source("internal/client/ui/AuiPageHost");
        assertTrue("Owned AUI pages must render the configured cursor", calls(host, "render", "UiPagePointer", "render"));
        assertTrue(calls(host, "pointerButton", "UiPagePointer", "button"));
        assertTrue(calls(host, "pointerScroll", "UiPagePointer", "scroll"));
        assertFalse("Screen overrides would accumulate unconsumed scroll twice", host.methods.stream().anyMatch(m -> m.name.equals("mouseScrolled")));
        assertTrue(calls(host, "removed", "UiPagePointer", "close"));
    }

    @Test
    public void bootstrapDoesNotLinkTheOptionalScreenClass() throws Exception {
        ClassNode bootstrap = source("internal/client/BaniraCodexClientBootstrap");
        assertFalse(bootstrap.methods.stream()
                .flatMap(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false))
                .anyMatch(i -> i instanceof TypeInsnNode && ((TypeInsnNode) i).desc.endsWith("AuiPageHost")));
    }

    @Test
    public void pooledPointerIsNotDrawnAgainByOtherHudPasses() throws Exception {
        assertTrue(source("internal/client/ui/SharedHudHosts").methods.stream().filter(m -> m.name.equals("renderOthers"))
                .flatMap(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false))
                .anyMatch(i -> i instanceof LdcInsnNode && "banira_codex:pointer".equals(((LdcInsnNode) i).cst)));
    }
}
