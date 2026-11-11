package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static org.junit.Assert.*;

public class UiCommonPageRoutesTest {
    @Test
    public void forgePointerFeedbackRetainsCanceledEventsAndLowestPriority() throws Exception {
        var node = read("internal/forge/client/BaniraClientForgeEventHandler");
        for (String name : java.util.List.of("onOwnedPointerButton", "onOwnedPointerScroll")) {
            var method = node.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            var annotation = method.visibleAnnotations.stream()
                    .filter(a -> a.desc.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")).findFirst().orElseThrow();
            int canceled = annotation.values.indexOf("receiveCanceled");
            int priority = annotation.values.indexOf("priority");
            assertTrue(canceled >= 0 && priority >= 0);
            assertEquals(Boolean.TRUE, annotation.values.get(canceled + 1));
            assertEquals("LOWEST", ((String[]) annotation.values.get(priority + 1))[1]);
            assertTrue(java.util.Arrays.stream(method.instructions.toArray()).anyMatch(i ->
                    i instanceof MethodInsnNode call && call.owner.equals("xin/vanilla/banira/internal/client/ui/UiPointerFeedback")));
        }
    }

    @Test
    public void clientDriverInstallsWithoutDirectlyResolvingTheOptionalRenderer() throws Exception {
        var node = read("internal/client/BaniraCodexClientBootstrap");
        var init = node.methods.stream().filter(m -> m.name.equals("init")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(init.instructions.toArray()).anyMatch(i ->
                i instanceof MethodInsnNode c && c.owner.equals("xin/vanilla/banira/api/client/ui/BaniraUi") && c.name.equals("install")));
        for (var method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof TypeInsnNode type)
                    assertFalse(type.desc.startsWith("com/sighs/apricityui/"));
    }

    private ClassNode read(String type) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + type + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }
}
