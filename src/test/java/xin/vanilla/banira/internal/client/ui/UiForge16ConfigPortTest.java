package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.junit.Assert.*;

public class UiForge16ConfigPortTest {

    private static ClassNode code(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = UiForge16ConfigPortTest.class.getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void nativeConfigExposesTheSharedSession() throws Exception {
        ClassNode screen = code("client/gui/ConfigEditorScreen");
        assertTrue(screen.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
        assertTrue(screen.methods.stream().anyMatch(method -> method.name.equals("uiPage")));
        ClassNode bootstrap = code("internal/client/BaniraCodexClientBootstrap");
        boolean routed = bootstrap.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray())).anyMatch(instruction -> instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/internal/client/ui/CommonUiPages") && ((MethodInsnNode) instruction).name.equals("tick"));
        assertTrue("Native and AUI config entry points must share ownership", routed);
    }

    @Test
    public void forge16RetainsItsExistingEventHub() throws Exception {
        ClassNode bootstrap = code("internal/client/BaniraCodexClientBootstrap");
        long calls = bootstrap.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray())).filter(instruction -> instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.startsWith("xin/vanilla/banira/client/event/BaniraClientEventHub$")).count();
        assertTrue("Do not replace the Forge 1.16.5 public event wrappers", calls >= 5);
    }
}
