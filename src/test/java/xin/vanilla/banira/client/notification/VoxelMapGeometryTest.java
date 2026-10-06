package xin.vanilla.banira.client.notification;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import static org.junit.Assert.*;
public class VoxelMapGeometryTest {
    @Test public void capturesActualPositionWithoutRadarOwnedLayoutFields() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in=getClass().getResourceAsStream("/xin/vanilla/banira/internal/mixin/compat/minimap/VoxelMapNotificationMixin.class")) {
            assertNotNull(in); new ClassReader(in).accept(node,0);
        }
        assertFalse("Radar-disabled layout fields are stale",node.fields.stream().anyMatch(f -> f.name.equals("layoutVariables")));
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("banira$appendUnread") && m.desc.startsWith("(Lnet/minecraft/client/gui/GuiGraphics;II")));
    }
}
