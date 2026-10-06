package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JourneyMapNotificationVisibilityTest {
    @Test
    public void supplierChecksMapEnabledBeforeClaimingHud() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream(
                "/xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        boolean guarded = false;
        for (MethodNode method : node.methods) {
            boolean checked = false;
            boolean branched = false;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("journeymap/client/ui/UIManager")
                            && call.name.equals("isMiniMapEnabled")) checked = true;
                    if (call.owner.endsWith("/NotificationMinimapBridge") && call.name.equals("text"))
                        guarded |= checked && branched;
                }
                if (checked && instruction.getOpcode() == Opcodes.IFEQ) branched = true;
            }
        }
        assertTrue("Hidden-map layout queries must not refresh HUD ownership", guarded);
    }
}
