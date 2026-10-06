package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NativeMinimapIntegrationTest {
    private ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull("Native adapter missing: " + name, in);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    @Test
    public void journeyRegistersSlotAndGuardsHiddenMinimap() throws Exception {
        ClassNode node = read("internal/neoforge/compat/minimap/JourneyMapNotificationPlugin");
        boolean register = false, guard = false, position = false;
        for (MethodNode m : node.methods)
            for (AbstractInsnNode i : m.instructions)
                if (i instanceof MethodInsnNode) {
                    String n = ((MethodInsnNode) i).name;
                    register |= n.equals("register");
                    guard |= n.equals("isMiniMapEnabled");
                    position |= n.equals("addLast");
                }
        assertTrue(register && guard && position);
    }

    @Test
    public void ftbUsesNativeInformationComponentApi() throws Exception {
        ClassNode node = read("internal/compat/minimap/FtbNotificationInfo");
        assertTrue(node.interfaces.contains("dev/ftb/mods/ftbchunks/api/client/minimap/MinimapInfoComponent"));
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("shouldRender")));
    }

    @Test
    public void atlasCapturesNativeTextCoordinatesWithoutLocalVariables() throws Exception {
        ClassNode node = read("internal/mixin/compat/minimap/MapAtlasesNotificationMixin");
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("banira$appendUnread") && m.desc.startsWith("(Lnet/minecraft/client/gui/GuiGraphics;IILpepjebs/mapatlases/client/Anchoring;")));
        assertTrue(node.fields.stream().anyMatch(f -> f.name.equals("globalScale") && f.desc.equals("F")));
    }
}
