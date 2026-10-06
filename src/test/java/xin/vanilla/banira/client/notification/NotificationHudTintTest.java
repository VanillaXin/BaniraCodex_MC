package xin.vanilla.banira.client.notification;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import static org.junit.Assert.*;

public class NotificationHudTintTest {
    @Test
    public void edgeTextureIsBoundBeforeTintWithoutASecondBindingDraw() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/client/notification/NotificationUnreadHud.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        boolean guarded = false;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("drawContents")) continue;
            boolean edge = false, bound = false, tinted = false;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).name.equals("ICON_EDGE")) edge = true;
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (edge && call.name.equals("setShaderTexture")) bound = true;
                if (bound && call.name.equals("setShaderColor")) tinted = true;
                if (tinted && call.name.equals("blitByBlend")) guarded = true;
            }
        }
        assertTrue("Texture binding resets shader color; tint must be applied after binding", guarded);
    }
}
