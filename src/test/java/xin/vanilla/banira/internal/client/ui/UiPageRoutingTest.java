package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiPageRoutingTest {
    @Test
    public void auiScreenHostUsesOfficialScreenOnlyBehindOptionalBackend() throws Exception {
        Class<?> backend = type("internal.client.ui.AuiUiBackend");
        assertNotNull(backend.getMethod("create", type("api.client.ui.UiPageSpec"), Object.class));
        try (java.io.InputStream input = getClass().getResourceAsStream(
                "/xin/vanilla/banira/internal/client/ui/AuiPageHost.class")) {
            assertNotNull("Missing actual AUI screen host", input);
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            assertEquals("com/sighs/apricityui/screen/ApricityScreen", node.superName);
            boolean logged = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods) {
                if (!method.name.equals("render")) continue;
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode
                            && ((org.objectweb.asm.tree.MethodInsnNode) instruction).name.equals("warn")) logged = true;
                }
            }
            assertTrue("Renderer fallback must retain a diagnostic", logged);
        }
    }

    @Test
    public void bootstrapRoutesSafeTicksThroughSessionHost() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream(
                "/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            boolean installed = false, ticked = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods) {
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                    if (!(instruction instanceof org.objectweb.asm.tree.MethodInsnNode)) continue;
                    org.objectweb.asm.tree.MethodInsnNode call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                    if (call.owner.equals("xin/vanilla/banira/api/client/ui/BaniraUi") && call.name.equals("install")) installed = true;
                    if (call.owner.equals("xin/vanilla/banira/internal/client/ui/UiSessionHost") && call.name.equals("tick")) ticked = true;
                }
            }
            assertTrue("Client UI driver must be installed", installed);
            assertTrue("Session handoff must run on safe client tick", ticked);
        }
    }
    @Test
    public void invalidNativeFactoryDoesNotReachPresenter() throws Exception {
        for (Object invalid : new Object[]{null, "not a screen"}) {
            Fixture f = new Fixture(false, (page, parent) -> view(new FakeScreen()), parent -> invalid);
            assertFalse(f.open());
            assertEquals(0, f.shown.size());
            assertEquals(0, f.bound.size());
        }
    }

    @Test
    public void recoverableAuiFailureFallsBackOnceWithoutChangingPreference() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        Fixture f = new Fixture(true, (page, parent) -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("test renderer failure");
        }, parent -> new FakeScreen());
        assertTrue(f.open());
        for (int i = 0; i < 100; i++) call(f.host, "tick");
        assertEquals(1, attempts.get());
        assertEquals(1, f.shown.size());
        assertFalse(f.backend.usesAui("banira_codex:test"));
        assertTrue(f.backend.wantsAui("banira_codex:other"));
    }

    @Test
    public void fatalRendererErrorsAreNotSwallowed() throws Exception {
        Fixture f = new Fixture(true, (page, parent) -> { throw new OutOfMemoryError("test sentinel"); },
                parent -> new FakeScreen());
        try {
            f.open();
            fail("Fatal renderer error was hidden");
        } catch (java.lang.reflect.InvocationTargetException error) {
            assertTrue(error.getCause() instanceof OutOfMemoryError);
        }
    }
}
