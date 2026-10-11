package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class JourneyMapNotificationVisibilityTest {
    @Test
    public void supplierChecksMapEnabledBeforeClaimingHud() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream(
                "/xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        JourneyMapGuardAssertions.assertNativeGuard(node, "journeymap/client/ui/UIManager", "isMiniMapEnabled");
    }
}
