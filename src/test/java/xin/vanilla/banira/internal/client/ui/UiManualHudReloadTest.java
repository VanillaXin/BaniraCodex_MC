package xin.vanilla.banira.internal.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiHudBinding;
import xin.vanilla.banira.api.client.ui.UiHudSpec;
import xin.vanilla.banira.api.client.ui.UiView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UiManualHudReloadTest {
    private static final String ID = "aotake_sweep:progress";

    private static final class View extends NativeUiView implements AuiHudHost.ManualRenderer {
        boolean documentCurrent = true;
        int draws;

        View() {
            super(null);
        }

        public boolean currentDocument() {
            return documentCurrent;
        }

        @Override
        public void renderHud(PoseStack graphics) {
            if (!documentCurrent) throw new IllegalStateException("Expired document");
            draws++;
        }
    }

    private static final class Binding implements UiHudBinding {
        boolean visible = true;
        int actions, binds;
        final List<UiDetachReason> reasons = new ArrayList<>();

        @Override
        public boolean visible() {
            return visible;
        }

        @Override
        public void bind(UiView view) {
            binds++;
            view.onAction("test", () -> actions++);
        }

        @Override
        public void detach(UiDetachReason reason) {
            reasons.add(reason);
        }
    }

    private static final class Fixture {
        final UiBackendController backend = new UiBackendController(() -> true, () -> true);
        final Binding binding = new Binding();
        final List<View> views = new ArrayList<>();
        int attempts;
        boolean brokenTemplate;
        final AuiHudHost host = new AuiHudHost(backend, spec -> {
            attempts++;
            if (brokenTemplate) throw new IllegalStateException("Invalid template");
            View view = new View();
            views.add(view);
            return view;
        });
        final AutoCloseable lease = host.register(new UiHudSpec(ID, "aotake_sweep/progress.html", binding, true, false));

        void tick(boolean connected) {
            backend.tick();
            host.tick(connected);
        }

        boolean render() {
            return host.render(ID, view -> ((View) view).renderHud(null));
        }
    }

    @Test
    public void refreshedDocumentRebindsWithoutSwitchingOrResettingBackend() {
        Fixture f = new Fixture();
        f.tick(true);
        assertTrue(f.render());
        View old = f.views.get(0);
        old.documentCurrent = false;
        assertTrue("A refreshed HUD must recover without toggling AUI", f.render());
        assertTrue(old.closed());
        assertEquals(2, f.binding.binds);
        assertEquals(List.of(UiDetachReason.RELOAD), f.binding.reasons);
        assertEquals(0, f.backend.revision());
        assertTrue(f.backend.usesAui(ID));
        old.activate("test");
        assertEquals(0, f.binding.actions);
        f.views.get(1).activate("test");
        assertEquals(1, f.binding.actions);
    }

    @Test
    public void repeatedReloadsKeepOneLiveViewAndDoNotRecreateStableFrames() {
        Fixture f = new Fixture();
        f.tick(true);
        assertTrue(f.render());
        for (int i = 0; i < 3; i++) {
            f.views.get(i).documentCurrent = false;
            assertTrue("Every template generation must recover", f.render());
            for (int frame = 0; frame < 5; frame++) assertTrue(f.render());
            assertEquals(i + 2, f.attempts);
        }
        assertEquals(1, f.views.stream().filter(view -> !view.closed()).count());
        assertEquals(List.of(UiDetachReason.RELOAD, UiDetachReason.RELOAD, UiDetachReason.RELOAD), f.binding.reasons);
    }

    @Test
    public void recoveryDoesNotOverrideNativePreferenceVisibilityOrDisconnect() throws Exception {
        Fixture f = new Fixture();
        f.tick(true);
        assertTrue(f.render());
        f.views.get(0).documentCurrent = false;
        f.backend.requestSwitch(false);
        f.tick(true);
        assertFalse(f.render());
        assertEquals(1, f.attempts);
        f.binding.visible = false;
        f.backend.requestSwitch(true);
        f.tick(true);
        assertFalse(f.render());
        assertEquals(1, f.attempts);
        f.binding.visible = true;
        f.tick(false);
        assertFalse(f.render());
        assertEquals(1, f.attempts);
        f.tick(true);
        assertTrue(f.render());
        f.lease.close();
        assertFalse(f.render());
        assertTrue(f.views.stream().allMatch(NativeUiView::closed));
    }

    @Test
    public void invalidReplacementRemainsIsolatedWithoutRetryLoop() {
        Fixture f = new Fixture();
        f.tick(true);
        assertTrue(f.render());
        f.views.get(0).documentCurrent = false;
        f.brokenTemplate = true;
        assertFalse(f.render());
        for (int frame = 0; frame < 5; frame++) assertFalse(f.render());
        assertEquals(2, f.attempts);
        assertTrue(f.views.get(0).closed());
        assertFalse(f.backend.wantsAui(ID));
        assertFalse(f.backend.usesAui(ID));
    }

    @Test
    public void genuineDrawFailuresStillUseNativeFallbackWithoutRetry() {
        Fixture f = new Fixture();
        f.tick(true);
        assertTrue(f.render());
        assertFalse(f.host.render(ID, view -> {
            throw new IllegalStateException("Draw failure");
        }));
        assertFalse(f.render());
        assertEquals(1, f.attempts);
        assertFalse(f.backend.wantsAui(ID));
    }

    @Test
    public void manualRecoveryDoesNotResetOtherSurfacesOrChangeAutomaticHudPolicy() {
        Fixture f = new Fixture();
        f.host.register(new UiHudSpec("test:automatic", "banira_codex/hud/overlay.html", new Binding()));
        f.tick(true);
        assertTrue(f.host.render("test:automatic", view -> {
        }));
        View automatic = f.views.get(0);
        automatic.documentCurrent = false;
        assertTrue(f.host.render("test:automatic", view -> assertSame(automatic, view)));
        assertFalse(f.host.render("test:automatic", view -> {
            throw new IllegalStateException("Other failure");
        }));
        assertTrue(f.render());
        f.views.get(1).documentCurrent = false;
        assertTrue(f.render());
        assertFalse(f.backend.wantsAui("test:automatic"));
        assertEquals(0, f.backend.revision());
        assertEquals(3, f.attempts);
    }

    @Test
    public void manualReadinessIsOptionalForExistingRenderers() {
        assertTrue(java.util.Arrays.stream(AuiHudHost.ManualRenderer.class.getMethods())
                .anyMatch(method -> method.getName().equals("currentDocument") && method.isDefault()));
    }

    @Test
    public void persistentHudAlsoTracksTheOfficialReloadedResourceCache() throws Exception {
        var type = new org.objectweb.asm.tree.ClassNode();
        try (var in = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ui/AuiHudDocument.class")) {
            new org.objectweb.asm.ClassReader(in).accept(type, 0);
        }
        var ready = type.methods.stream().filter(method -> method.name.equals("currentDocument")).findFirst().orElseThrow();
        assertTrue("Persistent documents keep their generation, so resource reload must also retire the view",
                java.util.Arrays.stream(ready.instructions.toArray()).anyMatch(instruction -> instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                        && call.owner.equals("com/sighs/apricityui/loader/ClientLoader") && call.name.equals("listFinalStaticResources")));
    }

    private org.objectweb.asm.tree.ClassNode official(String name) throws Exception {
        try (var jar = new java.util.zip.ZipFile(System.getProperty("banira.aui.contractJar"));
             var input = jar.getInputStream(jar.getEntry(name + ".class"))) {
            var type = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(type, 0);
            return type;
        }
    }

    @Test
    public void pinnedResourceListReturnsItsCachedIdentityBeforeAnyScan() throws Exception {
        var type = official("com/sighs/apricityui/loader/ClientLoader");
        var method = type.methods.stream().filter(value -> value.name.equals("listFinalStaticResources") && value.desc.equals("()Ljava/util/List;")).findFirst().orElseThrow();
        assertTrue((method.access & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0);
        assertTrue((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0);
        boolean cacheRead = false, cachedReturn = false;
        for (var instruction : method.instructions) {
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field && field.name.equals("cachedFinalStaticResources"))
                cacheRead = true;
            if (instruction.getOpcode() == org.objectweb.asm.Opcodes.ARETURN && cacheRead) cachedReturn = true;
            if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call && call.name.equals("loadResourcePackEntries")) {
                assertTrue("Stable HUD frames must not scan resources", cachedReturn);
                return;
            }
        }
        fail("Pinned resource cache contract changed");
    }

    @Test
    public void pinnedEngineSkipsPersistentDocumentsDuringGlobalRefresh() throws Exception {
        var type = official("com/sighs/apricityui/dom/DocumentRegistry");
        var method = type.methods.stream().filter(value -> value.name.equals("refreshAll")).findFirst().orElseThrow();
        assertTrue(java.util.Arrays.stream(method.instructions.toArray()).anyMatch(instruction -> instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                && call.name.equals("isReloadPersistent")));
    }
}
