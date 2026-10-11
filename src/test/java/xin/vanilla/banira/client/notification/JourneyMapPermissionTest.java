package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JourneyMapPermissionTest {
    private ClassNode read() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    @Test
    public void supplierChecksNativePermissionBeforeClaimingHud() throws Exception {
        ClassNode node = read();
        JourneyMapGuardAssertions.assertNativeGuard(node, "journeymap/client/InternalStateHandler", "isMinimapEnabled");
    }

    @Test
    public void permissionTransitionIsTrackedForLayoutRefresh() throws Exception {
        ClassNode node = read();
        assertTrue("Restoring permission must rebuild previously empty slots", node.fields.stream().anyMatch(f -> f.name.equals("previousAllowed") && f.desc.equals("Z")));
        MethodNode refresh = node.methods.stream().filter(m -> m.name.equals("refreshLayout")).findFirst().get();
        boolean read = false, write = false;
        for (AbstractInsnNode i : refresh.instructions)
            if (i instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) i;
                if (field.name.equals("previousAllowed")) {
                    read |= field.getOpcode() == 180;
                    write |= field.getOpcode() == 181;
                }
            }
        assertTrue(read && write);
    }
}
