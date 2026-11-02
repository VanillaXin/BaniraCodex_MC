package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import static org.junit.Assert.*;

public class UiPointerEventDispatchTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void cancelledInputIsObservedWithoutReplayingBusinessInput() throws Exception {
        ClassNode events = source("internal/forge/client/ForgeBaniraClientEventBridge");
        for (String method : new String[]{"onOwnedPointerButton", "onOwnedPointerScroll"}) {
            MethodNode observer = events.methods.stream().filter(m -> m.name.equals(method)).findFirst().orElse(null);
            assertNotNull("AUI-consumed input needs an independent observer: " + method, observer);
            AnnotationNode subscribe = observer.visibleAnnotations.stream()
                    .filter(a -> a.desc.equals("Lnet/minecraftforge/eventbus/api/SubscribeEvent;")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
            assertEquals(Boolean.TRUE, subscribe.values.get(subscribe.values.indexOf("receiveCanceled") + 1));
            String[] priority = (String[]) subscribe.values.get(subscribe.values.indexOf("priority") + 1);
            assertEquals("LOWEST", priority[1]);
            boolean feedback = false;
            for (AbstractInsnNode instruction : observer.instructions)
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.endsWith("UiPointerFeedback")) feedback = true;
                    assertFalse("Pointer feedback must not replay screen input", call.name.equals("mouseClicked")
                            || call.name.equals("mouseReleased") || call.name.equals("mouseScrolled"));
                    assertFalse("Pointer observation must not change cancellation", call.name.equals("setCanceled"));
                }
            assertTrue("Cancelled events must reach the owned feedback interface", feedback);
        }
    }

    @Test
    public void feedbackContractHasNoLoaderOrEngineDescriptors() {
        for (lombok.var method : UiPointerFeedback.class.getDeclaredMethods()) {
            assertTrue(method.getReturnType().isPrimitive());
            for (Class<?> parameter : method.getParameterTypes()) assertTrue(parameter.isPrimitive());
        }
    }

    @Test
    public void nativeReinitReleasesOldCursorBeforeReplacement() throws Exception {
        MethodNode init = source("client/gui/BaniraScreen").methods.stream().filter(m -> m.name.equals("init")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        boolean released = false, replacement = false;
        for (AbstractInsnNode instruction : init.instructions)
            if (instruction instanceof MethodInsnNode
                    && ((MethodInsnNode) instruction).owner.endsWith("MouseWidget")) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.name.equals("removed")) released = true;
                if (call.name.equals("init")) {
                    assertTrue("Reinit must release the discarded cursor token", released);
                    replacement = true;
                }
            }
        assertTrue(replacement);
    }
}
