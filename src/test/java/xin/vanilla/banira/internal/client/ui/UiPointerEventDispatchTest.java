package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import static org.junit.Assert.*;

public class UiPointerEventDispatchTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void pointerOverlayHasItsOwnForegroundStratum() throws Exception {
        MethodNode render = source("internal/client/ui/UiPagePointer").methods.stream()
                .filter(m -> m.name.equals("render")).findFirst().orElseThrow();
        boolean separated = false, painted = false, finished = false;
        for (AbstractInsnNode instruction : render.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.name.equals("nextStratum")) {
                if (painted) finished = true;
                else separated = true;
            }
            if (call.owner.endsWith("SharedHudHosts") && call.name.equals("render")) {
                assertTrue(separated);
                painted = true;
            }
        }
        assertTrue(painted && finished);
        assertFalse(render.tryCatchBlocks.isEmpty());
    }

    @Test
    public void pointerHudSubmitsTheForegroundPlaneToTheNativePipRenderer() throws Exception {
        MethodNode render = source("internal/client/ui/AuiHudDocument").methods.stream()
                .filter(m -> m.name.equals("render")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(render.instructions.toArray()).anyMatch(instruction ->
                instruction instanceof MethodInsnNode call && call.owner.endsWith("AuiDocumentRenderSubmission")
                        && call.name.equals("submit") && call.desc.endsWith(";Z)V")));
        MethodNode rasterize = source("internal/compat/aui/AuiDocumentRenderSubmission$Renderer").methods.stream()
                .filter(m -> m.name.equals("renderToTexture") && (m.access & org.objectweb.asm.Opcodes.ACC_BRIDGE) == 0)
                .findFirst().orElseThrow();
        boolean foreground = false, explicitPlane = false;
        for (AbstractInsnNode instruction : rasterize.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) continue;
            if (call.owner.endsWith("/Base") && call.name.equals("getFlatOverlayZ")) foreground = true;
            if (call.owner.endsWith("/Base") && call.name.equals("drawOverlayDocument") && call.desc.endsWith(";F)V")) {
                assertTrue(foreground);
                explicitPlane = true;
            }
        }
        assertTrue(explicitPlane);
    }

    @Test
    public void cancelledInputIsObservedWithoutReplayingBusinessInput() throws Exception {
        ClassNode events = source("internal/fabric/client/FabricAuiClientHooks");
        for (String method : new String[]{"observeButton", "observeScroll"}) {
            MethodNode observer = events.methods.stream().filter(m -> m.name.equals(method)).findFirst().orElse(null);
            assertNotNull("AUI-consumed input needs an independent observer: " + method, observer);
            boolean feedback = false;
            for (AbstractInsnNode instruction : observer.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.endsWith("UiPointerFeedback")) feedback = true;
                    assertFalse("Pointer feedback must not replay screen input", call.name.equals("mouseClicked")
                            || call.name.equals("mouseReleased") || call.name.equals("mouseScrolled"));
                    assertFalse("Pointer observation must not change cancellation", call.name.equals("setCanceled"));
                }
            assertTrue("Cancelled events must reach the owned feedback interface", feedback);
        }
        ClassNode mixin = source("internal/mixin/injections/AuiPointerInputMixin");
        AnnotationNode declaration = mixin.invisibleAnnotations.stream()
                .filter(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElseThrow();
        assertEquals(2000, declaration.values.get(declaration.values.indexOf("priority") + 1));
        for (MethodNode method : mixin.methods) {
            if (!method.name.startsWith("banira$observe")) continue;
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call)
                    assertFalse("Observation must not cancel the event", call.name.equals("cancel") || call.name.equals("setReturnValue"));
        }
    }

    @Test
    public void feedbackContractHasNoLoaderOrEngineDescriptors() {
        for (var method : UiPointerFeedback.class.getDeclaredMethods()) {
            assertTrue(method.getReturnType().isPrimitive());
            for (Class<?> parameter : method.getParameterTypes()) assertTrue(parameter.isPrimitive());
        }
    }

    @Test
    public void nativeReinitReleasesOldCursorBeforeReplacement() throws Exception {
        MethodNode init = source("client/gui/BaniraScreen").methods.stream().filter(m -> m.name.equals("init")).findFirst().orElseThrow();
        boolean released = false, replacement = false;
        for (AbstractInsnNode instruction : init.instructions)
            if (instruction instanceof MethodInsnNode call
                    && call.owner.endsWith("MouseWidget")) {
                if (call.name.equals("removed")) released = true;
                if (call.name.equals("init")) {
                    assertTrue("Reinit must release the discarded cursor token", released);
                    replacement = true;
                }
            }
        assertTrue(replacement);
    }
}
