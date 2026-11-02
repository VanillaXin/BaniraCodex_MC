package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class UiFabric19CoreWiringTest {
    private String bootstrap() throws Exception {
        return Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.java"));
    }

    @Test
    public void publicDriverUsesTheSharedOwnerAndContainerGate() throws Exception {
        String source = bootstrap();
        assertTrue(source.contains("BaniraUi.install("));
        assertTrue(source.contains("UI_SESSIONS.open(page, previousScreen)"));
        assertTrue(source.contains("if (!UI_SESSIONS.ownsScreen(screen)) return false"));
        assertTrue(source.contains("UiContainerLayerHost.create(UI_BACKEND, page, current,"));
        assertTrue(source.contains("UI_SESSIONS.screenRemoved(screen)"));
    }

    @Test
    public void endTickObservesTheCurrentScreenBeforeAdvancingSessions() throws Exception {
        String source = bootstrap();
        int end = source.indexOf("if (event != BaniraClientTickEvent.END) return;");
        int observe = source.indexOf("UI_SESSIONS.observeScreen(BaniraClientRuntime.currentScreen())");
        int tick = source.indexOf("UI_SESSIONS.tick()");
        assertTrue(end >= 0 && observe > end && tick > observe);
        assertTrue(source.contains("if (hadConnection && !connected) UI_SESSIONS.disconnect()"));
        assertTrue(source.contains("onClientLoggedOut(player ->"));
    }

    @Test
    public void resourceAndPreferenceChangesInvalidateTheSharedHosts() throws Exception {
        String source = bootstrap();
        assertTrue(source.contains("UI_SESSIONS.switchBackend(ClientConfigView.get().useAui())"));
        assertTrue(source.contains("onTextureReload(event ->"));
        assertTrue(source.contains("AUI.reload()"));
        assertTrue(source.contains("UI_SESSIONS.reload()"));
        assertFalse(source.contains("SharedHudHosts.register("));
    }

    @Test
    public void nativeScreenPresentationKeepsItsTypeBoundary() throws Exception {
        String source = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/client/BaniraClientRuntime.java"));
        assertTrue(source.contains("return screen instanceof Screen"));
        assertTrue(source.contains("if (screen != null && !isScreen(screen)) throw new IllegalArgumentException"));
        assertTrue(source.contains("setScreen((Screen) screen)"));
        assertTrue(source.contains("Minecraft.getInstance().setScreen(screen)"));
    }

    @Test
    public void pointerSelectorsMatchTheActualMinecraft19MouseHandler() throws Exception {
        var node = new org.objectweb.asm.tree.ClassNode();
        try (var input = getClass().getResourceAsStream("/net/minecraft/client/MouseHandler.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, org.objectweb.asm.ClassReader.SKIP_CODE);
        }
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("onPress") && method.desc.equals("(JIII)V")));
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("onScroll") && method.desc.equals("(JDD)V")));
        String source = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/mixin/injections/AuiPointerInputMixin.java"));
        assertTrue(source.contains("onPress(JIII)V"));
        assertTrue(source.contains("onScroll(JDD)V"));
        assertFalse(source.contains("remap = false"));
        assertFalse(source.contains("cancellable"));
        String metadata = Files.readString(Path.of("src/main/resources/banira_codex.mixins.json"));
        assertTrue(metadata.contains("injections.AuiPointerInputMixin"));
    }

    @Test
    public void fabricRenderHooksOnlyUseTheCurrentScreenAndOwnedWindow() throws Exception {
        String hooks = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricAuiClientHooks.java"));
        assertTrue(hooks.contains("window == client.getWindow().getWindow()"));
        assertTrue(hooks.contains("client.screen instanceof UiPointerFeedback pointer"));
        assertTrue(hooks.contains("screen == Minecraft.getInstance().screen"));
        String entry = Files.readString(Path.of("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java"));
        assertTrue(entry.contains("FabricAuiClientHooks.renderScreen(scr, stack)"));
        assertTrue(entry.contains("FabricAuiClientHooks.renderGame(stack)"));
    }
}
