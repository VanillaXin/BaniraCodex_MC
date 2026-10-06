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
        ClassNode node = read("internal/forge/compat/minimap/JourneyMapNotificationPlugin");
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
}
