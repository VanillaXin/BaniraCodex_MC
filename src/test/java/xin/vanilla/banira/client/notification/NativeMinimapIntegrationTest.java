package xin.vanilla.banira.client.notification;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import static org.junit.Assert.*;
public class NativeMinimapIntegrationTest {
    private ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull("Native adapter missing: " + name, in);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }
    @Test public void ftbUsesComponentApiWithoutLegacyTextList() throws Exception {
        ClassNode node = read("internal/compat/minimap/FtbNotificationInfo");
        assertTrue(node.interfaces.contains("dev/ftb/mods/ftbchunks/api/client/minimap/MinimapInfoComponent"));
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("shouldRender")));
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("render")));
        assertFalse(node.fields.stream().anyMatch(f -> f.name.equals("MINIMAP_TEXT_LIST")));
    }
    @Test public void journeyRegistersSlotAndGuardsHiddenMinimap() throws Exception {
        ClassNode node = read("internal/fabric/compat/minimap/JourneyMapNotificationPlugin");
        boolean register=false, guard=false, position=false;
        for (MethodNode m: node.methods) for (AbstractInsnNode i: m.instructions) {
            if(i instanceof MethodInsnNode) {
                String name=((MethodInsnNode)i).name;
                register|=name.equals("register"); guard|=name.equals("isMiniMapEnabled"); position|=name.equals("addLast");
            }
        }
        assertTrue(register && guard && position);
    }
    @Test public void atlasUsesNativeGuiGraphicsAndTailGeometry() throws Exception {
        ClassNode node = read("internal/mixin/compat/minimap/MapAtlasesNotificationMixin");
        assertTrue(node.methods.stream().anyMatch(m -> m.name.equals("banira$appendUnread") && m.desc.contains("Lnet/minecraft/client/gui/GuiGraphics;")));
        assertTrue(node.fields.stream().anyMatch(f -> f.name.equals("globalScale") && f.desc.equals("F")));
    }
}
