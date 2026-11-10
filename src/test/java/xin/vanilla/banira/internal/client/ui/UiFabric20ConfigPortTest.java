package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class UiFabric20ConfigPortTest {
    private String source(String relative) throws Exception {
        return Files.readString(Path.of("src/main/java/xin/vanilla/banira/" + relative + ".java"));
    }

    @Test
    public void nativeConfigImplementsTheSharedPageContract() throws Exception {
        ClassNode type = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/client/gui/ConfigEditorScreen.class")) {
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
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(type, 0);
        }
        long hooks = type.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.startsWith("xin/vanilla/banira/internal/client/BaniraClientEventHub$")).count();
        assertTrue(hooks >= 5);
        assertFalse(source("internal/client/BaniraCodexClientBootstrap").contains("net.minecraftforge"));
    }
}
