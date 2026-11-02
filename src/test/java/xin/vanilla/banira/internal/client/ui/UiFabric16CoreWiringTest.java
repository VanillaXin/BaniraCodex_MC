package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class UiFabric16CoreWiringTest {
    private String bootstrap() throws Exception {
        return new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.java")), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    public void publicDriverUsesTheSharedOwnerAndContainerGate() throws Exception {
        String source = bootstrap();
        assertTrue(source.contains("BaniraUi.install("));
        assertTrue(source.contains("UI_SESSIONS.open(page, previousScreen)"));
        String pages = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/client/ui/CommonUiPages.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("CommonUiPages.close(screen, reason)"));
        assertTrue(pages.contains("if (!host.ownsScreen(screen)) return false"));
        assertTrue(source.contains("UiContainerLayerHost.create(UI_BACKEND, page, current,"));
        assertTrue(source.contains("CommonUiPages.removed(screen)"));
        assertTrue(pages.contains("uiSessions().screenRemoved(screen)"));
    }

    @Test
    public void endTickObservesTheCurrentScreenBeforeAdvancingSessions() throws Exception {
        String source = bootstrap();
        int end = source.indexOf("if (event != BaniraClientTickEvent.END) return;");
        int observe = source.indexOf("CommonUiPages.tick()");
        int tick = source.indexOf("UI_SESSIONS.tick()");
        assertTrue(end >= 0 && observe > end && tick > observe);
        assertTrue(source.contains("if (hadConnection && !connected) disconnectUiPages()"));
        assertTrue(source.contains("private static void disconnectUiPages()"));
        assertTrue(source.contains("CommonUiPages.disconnect()"));
        assertTrue(source.contains("finally {\n            xin.vanilla.banira.internal.config.CustomConfig.clearPlayerPreferencesClient()")
                || source.contains("finally {\r\n            xin.vanilla.banira.internal.config.CustomConfig.clearPlayerPreferencesClient()"));
        String pages = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/client/ui/CommonUiPages.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(pages.contains("sessions.observeScreen(current)"));
        assertTrue(pages.contains("NATIVE_SESSIONS.disconnect()"));
        assertTrue(source.contains("onClientLoggedOut(player ->"));
    }

    @Test
    public void resourceAndPreferenceChangesInvalidateTheSharedHosts() throws Exception {
        String source = bootstrap();
        assertTrue(source.contains("UI_SESSIONS.switchBackend(ClientConfigView.get().useAui())"));
        assertTrue(source.contains("onTextureReload(event ->"));
        assertTrue(source.contains("AUI.reload()"));
        assertTrue(source.contains("UI_SESSIONS.reload()"));
        assertEquals(4, source.split("SharedHudHosts.register\\(", -1).length - 1);
        assertTrue(source.contains("SharedHudHosts.NOTIFICATIONS"));
        assertTrue(source.contains("SharedHudHosts.UNREAD"));
    }

    @Test
    public void nativeScreenPresentationKeepsItsTypeBoundary() throws Exception {
        String source = bootstrap();
        assertTrue(source.contains("return screen instanceof net.minecraft.client.gui.screens.Screen"));
        assertTrue(source.contains("if (screen != null && !isScreen(screen)) throw new IllegalArgumentException"));
        assertTrue(source.contains("setScreen((net.minecraft.client.gui.screens.Screen) screen)"));
        assertTrue(source.contains("BaniraCodexClientBootstrap::presentUiScreen"));
    }

    @Test
    public void nativeTitleScreenPagesClearBeforePaintingTheirContents() throws Exception {
        String source = new String(Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/BaniraScreen.java")), java.nio.charset.StandardCharsets.UTF_8);
        int render = source.indexOf("public void render(PoseStack");
        int guard = source.indexOf("if (this.minecraft == null || this.minecraft.level == null)", render);
        int clear = source.indexOf("this.renderBackground(stack)", guard);
        int contents = source.indexOf("this.onRender(stack, partialTicks)", render);
        assertTrue(render >= 0 && guard > render && clear > guard && contents > clear);
    }

    @Test
    public void pointerSelectorsMatchTheActualMinecraft16MouseHandler() throws Exception {
        lombok.var node = new org.objectweb.asm.tree.ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/net/minecraft/client/MouseHandler.class")) {
            assertNotNull(input);
            new org.objectweb.asm.ClassReader(input).accept(node, org.objectweb.asm.ClassReader.SKIP_CODE);
        }
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("onPress") && method.desc.equals("(JIII)V")));
        assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("onScroll") && method.desc.equals("(JDD)V")));
        String source = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/mixin/injections/AuiPointerInputMixin.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("onPress(JIII)V"));
        assertTrue(source.contains("onScroll(JDD)V"));
        assertFalse(source.contains("remap = false"));
        assertFalse(source.contains("cancellable"));
        String metadata = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/banira_codex.mixins.json")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(metadata.contains("injections.AuiPointerInputMixin"));
    }

    @Test
    public void fabricRenderHooksOnlyUseTheCurrentScreenAndOwnedWindow() throws Exception {
        String hooks = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricAuiClientHooks.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(hooks.contains("window == client.getWindow().getWindow()"));
        assertTrue(hooks.contains("client.screen instanceof UiPointerFeedback"));
        assertTrue(hooks.contains("((UiPointerFeedback) client.screen).pointerButton"));
        assertTrue(hooks.contains("screen == Minecraft.getInstance().screen"));
        String entry = new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java")), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(entry.contains("FabricAuiClientHooks.renderScreen(scr, stack)"));
        assertTrue(entry.contains("FabricAuiClientHooks.renderGame(stack)"));
    }
}
