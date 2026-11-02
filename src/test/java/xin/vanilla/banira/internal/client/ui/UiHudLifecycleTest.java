package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

import static org.junit.Assert.*;

public class UiHudLifecycleTest {
    private ClassNode source(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    @Test
    public void documentOwnerCannotRecursivelyReenterDomCurrentCheck() throws Exception {
        ClassNode node = source("internal/client/ui/AuiHudDocument");
        for (MethodNode method : node.methods)
            if (method.name.startsWith("lambda$")) {
                for (lombok.var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        assertFalse("Document current predicate must not reenter UiDomView.live", call.name.equals("live"));
                    }
            }
        for (MethodNode method : node.methods)
            for (lombok.var instruction : method.instructions)
                if (instruction instanceof InvokeDynamicInsnNode) {
                    lombok.var dynamic = (InvokeDynamicInsnNode) instruction;
                    for (Object arg : dynamic.bsmArgs)
                        if (arg instanceof Handle) assertNotEquals("live", ((Handle) arg).getName());
                }
    }

    @Test
    public void persistentDocumentUsesInstanceDisposalAndNoMouseRelease() throws Exception {
        ClassNode node = source("internal/client/ui/AuiHudDocument");
        boolean manual = false, overlay = false;
        for (lombok.var method : node.methods)
            for (lombok.var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    lombok.var call = (MethodInsnNode) instruction;
                    if (call.name.equals("setManuallyRendered")) manual = true;
                    if (call.name.equals("drawOverlayDocument")) overlay = true;
                    assertFalse(call.name.equals("releaseMouse") || call.name.equals("setScreen") || call.name.equals("drawPseudoCursor"));
                    if (call.name.equals("remove") && call.owner.endsWith("/Document"))
                        assertEquals("Instance ownership only", "()V", call.desc);
                }
        assertTrue(manual);
        assertTrue(overlay);
    }

    @Test
    public void nativePointerHasOneGraphicsAwarePersistentEntrypoint() {
        boolean found = Arrays.stream(xin.vanilla.banira.client.gui.widget.MouseWidget.class.getMethods()).anyMatch(m ->
                m.getName().equals("draw") && Arrays.equals(m.getParameterTypes(), new Class[]{com.mojang.blaze3d.matrix.MatrixStack.class, int.class, int.class}));
        assertTrue("Pointer must share its original state with the persistent renderer", found);
    }

    @Test
    public void customRegisteredHudsAreIncludedInTheRenderPhase() {
        assertTrue("Public registrations must reach an actual render phase", Arrays.stream(AuiHudHost.class.getMethods()).anyMatch(m -> m.getName().equals("renderOthers")));
    }

    @Test
    public void bothExclusiveNativeRenderPhasesDispatchAutomaticHudsOnce() throws Exception {
        ClassNode hub = source("client/event/BaniraClientEventHub$Client");
        for (String name : Arrays.asList("fireDrawScreenPostNative", "fireRenderOverlayPostNative")) {
            int methods = 0;
            int calls = 0;
            for (MethodNode method : hub.methods) {
                if (!name.equals(method.name)) continue;
                methods++;
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("xin/vanilla/banira/internal/client/ui/SharedHudHosts")
                            && call.name.equals("renderOthers")) calls++;
                }
            }
            assertEquals(name, 1, methods);
            assertEquals(name, 1, calls);
        }
    }

    @Test
    public void automaticDispatchSkipsBuiltinsAndManualRegistrations() throws Exception {
        java.util.List<String> drawn = new java.util.ArrayList<>();
        class View extends NativeUiView implements SharedHudHosts.Renderer {
            private final String id;

            View(String id) {
                super(null);
                this.id = id;
            }

            public void render(com.mojang.blaze3d.matrix.MatrixStack graphics, HudDrawList frame) {
                drawn.add(id);
            }
        }
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        AuiHudHost host = new AuiHudHost(backend, spec -> new View(spec.id()));
        java.util.List<AutoCloseable> leases = new java.util.ArrayList<>();
        SharedHudHosts.install(host);
        try {
            for (String id : Arrays.asList(SharedHudHosts.NOTIFICATIONS, SharedHudHosts.UNREAD,
                    SharedHudHosts.QUICK_ACTIONS, SharedHudHosts.POINTER, "test:auto", "test:manual")) {
                leases.add(host.register(new xin.vanilla.banira.api.client.ui.UiHudSpec(id,
                        "banira_codex/hud/overlay.html", new UiHudOwnershipTest.Binding(), "test:manual".equals(id))));
            }
            backend.tick();
            host.tick(true);
            SharedHudHosts.renderOthers(new com.mojang.blaze3d.matrix.MatrixStack());
            assertEquals(Arrays.asList("test:auto"), drawn);
            drawn.clear();
            host.tick(false);
            SharedHudHosts.renderOthers(new com.mojang.blaze3d.matrix.MatrixStack());
            assertTrue("Disconnect must not keep drawing a detached HUD", drawn.isEmpty());
        } finally {
            SharedHudHosts.install(null);
            for (AutoCloseable lease : leases) lease.close();
        }
    }
}
