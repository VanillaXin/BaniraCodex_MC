package xin.vanilla.banira.internal.client.ui;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UiCommonPagesPortTest {
    private ClassNode node(String name) throws Exception {
        try (var in = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, in);
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            return node;
        }
    }

    private boolean calls(ClassNode node, String owner, String name) {
        for (var method : node.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name))
                    return true;
        return false;
    }

    @Test
    public void bootstrapInstallsRealSessionsAndFourOwnedHudSurfaces() throws Exception {
        var bootstrap = node("internal/client/BaniraCodexClientBootstrap");
        assertTrue(calls(bootstrap, "xin/vanilla/banira/api/client/ui/BaniraUi", "install"));
        assertTrue(calls(bootstrap, "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "install"));
        long registrations = bootstrap.methods.stream()
                .flatMap(method -> java.util.stream.StreamSupport.stream(method.instructions.spliterator(), false))
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals("xin/vanilla/banira/internal/client/ui/SharedHudHosts") && call.name.equals("register"))
                .count();
        assertEquals(4L, registrations);
        for (String method : List.of("tick", "disconnect"))
            assertTrue(calls(bootstrap, "xin/vanilla/banira/internal/client/ui/CommonUiPages", method));
        assertTrue(calls(bootstrap, "xin/vanilla/banira/internal/client/ui/UiSessionHost", "reload"));
    }

    @Test
    public void fabricPointerObserversDoNotReplayInput() throws Exception {
        var handler = node("internal/fabric/client/FabricAuiClientHooks");
        for (String name : List.of("observeButton", "observeScroll")) {
            var method = handler.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            boolean feedback = false;
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse("Observer must not replay screen input", List.of("mouseClicked", "mouseReleased", "mouseScrolled", "setCanceled").contains(call.name));
                    if (call.owner.equals("xin/vanilla/banira/internal/client/ui/UiPointerFeedback")) feedback = true;
                }
            assertTrue(feedback);
        }
    }

    @Test
    public void textureReloadUsesTheFabricAtlasUploadTail() throws Exception {
        String mixins = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/banira_codex.mixins.json"));
        assertTrue(mixins.contains("injections.AuiTextureAtlasMixin"));
        var setup = node("internal/mixin/injections/AuiTextureAtlasMixin");
        assertTrue(calls(setup, "xin/vanilla/banira/internal/client/BaniraClientEventHub$Client", "fireTextureReload"));
        assertTrue(setup.methods.stream().anyMatch(method -> method.name.equals("location")));
    }

    @Test
    public void hudRenderingHasSeparateGameAndScreenPhases() throws Exception {
        var handler = node("internal/fabric/client/FabricAuiClientHooks");
        for (String name : List.of("renderScreen", "renderGame")) {
            var method = handler.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
            int renders = 0, guards = 0;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.equals("xin/vanilla/banira/internal/client/ui/SharedHudHosts") && call.name.equals("renderOthers"))
                    renders++;
                if (instruction instanceof JumpInsnNode) guards++;
            }
            assertEquals(1, renders);
            assertTrue("Render phase must be screen-ownership guarded", guards > 0);
        }
        var bootstrap = node("internal/fabric/client/FabricBaniraCodexClient");
        assertTrue(calls(bootstrap, "xin/vanilla/banira/internal/fabric/client/FabricAuiClientHooks", "renderScreen"));
        assertTrue(calls(bootstrap, "xin/vanilla/banira/internal/fabric/client/FabricAuiClientHooks", "renderGame"));
    }

    @Test
    public void disconnectClearsOnlyClientPlayerCache() {
        JsonObject old = CustomConfig.getClientConfig();
        JsonObject serverBefore = CustomConfig.getCustomConfig().deepCopy();
        try {
            JsonObject cache = new JsonObject();
            cache.add("player", new JsonObject());
            cache.addProperty("other", "retained");
            CustomConfig.setClientConfig(cache);
            CustomConfig.clearPlayerPreferencesClient();
            CustomConfig.clearPlayerPreferencesClient();
            assertFalse(cache.has("player"));
            assertEquals("retained", cache.get("other").getAsString());
            assertEquals(serverBefore, CustomConfig.getCustomConfig());
        } finally {
            CustomConfig.setClientConfig(old);
        }
    }

    @Test
    public void missingEngineKeepsContainerAndHudNative() throws Exception {
        assertNull(getClass().getClassLoader().getResource("com/sighs/apricityui/screen/ApricityScreen.class"));
        var backend = new UiBackendController(() -> true, () -> true);
        var aui = new AuiUiBackend(() -> {
            throw new AssertionError("No page session requested");
        });
        var session = new UiPageSession() {
            public void bind(UiView view) {
                fail("Unavailable container must not bind");
            }

            public void detach(UiDetachReason reason) {
            }

            public void close(UiCloseReason reason) {
                fail("Native inventory still owns its session");
            }
        };
        var page = new UiPageSpec("test:container", "test/container.html", session, previous -> new Object());
        backend.tick();
        assertNull(UiContainerLayerHost.create(backend, page, () -> true, () -> aui.createContainer(page)));
        assertFalse(backend.wantsAui(page.id()));
        AtomicInteger binds = new AtomicInteger();
        var hud = new UiHudSpec("test:hud", "test/hud.html", new UiHudBinding() {
            public boolean visible() {
                return true;
            }

            public void bind(UiView view) {
                binds.incrementAndGet();
            }

            public void detach(UiDetachReason reason) {
            }
        });
        var host = new AuiHudHost(backend, aui::createHud);
        host.register(hud);
        host.tick(true);
        assertFalse(host.render(hud.id(), view -> fail("No engine must preserve native HUD")));
        assertEquals(0, binds.get());
        assertFalse(backend.wantsAui(hud.id()));
    }

    @Test
    public void commonDocumentsRemainRealFactoriesWithoutEngineLeak() throws Exception {
        for (String type : List.of("AuiPageHost", "AuiHudDocument", "AuiContainerDocument")) {
            var document = node("internal/client/ui/" + type);
            assertTrue(document.methods.stream().anyMatch(m -> m.name.equals("create") && (m.access & Opcodes.ACC_STATIC) != 0));
        }
        var backend = node("internal/client/ui/AuiUiBackend");
        for (String name : List.of("create", "createHud", "createContainer"))
            assertTrue(backend.methods.stream().anyMatch(m -> m.name.equals(name)));
        assertFalse(backend.fields.stream().anyMatch(f -> f.desc.startsWith("Lcom/sighs/")));
    }
}
