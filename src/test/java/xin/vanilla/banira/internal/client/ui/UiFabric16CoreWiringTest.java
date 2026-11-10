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


}
