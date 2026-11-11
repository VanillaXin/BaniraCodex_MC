package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;

import static org.junit.Assert.*;

public class UiFabric16ConfigPortTest {
    private String source(String relative) throws Exception {
        return new String(Files.readAllBytes(java.nio.file.Paths.get("src/main/java/xin/vanilla/banira/" + relative + ".java")), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    public void nativeConfigImplementsTheSharedPageContract() throws Exception {
        ClassNode type = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/client/gui/ConfigEditorScreen.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        assertTrue(type.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
        assertTrue(type.methods.stream().anyMatch(method -> method.name.equals("uiPage")));
        assertTrue(type.methods.stream().anyMatch(method -> method.name.equals("captureUiState")));
    }


    @Test
    public void configurationLifecycleKeepsFabricEventRegistration() throws Exception {
        ClassNode type = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        long hooks = type.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.startsWith("xin/vanilla/banira/client/event/BaniraClientEventHub$")).count();
        assertTrue(hooks >= 5);
        assertFalse(source("internal/client/BaniraCodexClientBootstrap").contains("net.minecraftforge"));
    }

    @Test
    public void nativeKeyCaptureKeepsTheMinecraft16EventBoundary() throws Exception {
        Class<?> widget = xin.vanilla.banira.client.gui.widget.KeyCaptureInputWidget.class;
        Class<?> event = xin.vanilla.banira.client.gui.event.KeyEvent.class;
        assertNotNull(widget.getDeclaredMethod("onKeyPress", event));
        assertNotNull(widget.getDeclaredMethod("onKeyRelease", event));
        String source = source("client/gui/widget/KeyCaptureInputWidget");
        assertTrue(source.contains("int keyCode = event.keyCode()"));
        assertTrue(source.contains("pending.release(keyCode)"));
    }


}
