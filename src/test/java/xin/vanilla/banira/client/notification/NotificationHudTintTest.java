package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NotificationHudTintTest {
    @Test
    public void edgeTintIsCapturedInOneTexturedExtractionWithoutGlobalShaderState() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/client/notification/NotificationUnreadHud.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        int draws = 0;
        boolean edge = false, readable = false;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("drawContents")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).name.equals("ICON_EDGE"))
                    edge = true;
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                org.junit.Assert.assertFalse("Extraction must not leak global shader tint",
                        call.owner.equals("com/mojang/blaze3d/systems/RenderSystem"));
                if (call.name.equals("ensureReadableTextArgb")) readable = true;
                if (edge && call.owner.equals("net/minecraft/client/gui/GuiGraphicsExtractor")
                        && call.name.equals("blit")) {
                    assertTrue("The textured render state must include its own ARGB tint", call.desc.endsWith("I)V"));
                    assertTrue("Readability adjustment must precede tinted extraction", readable);
                    draws++;
                }
            }
        }
        org.junit.Assert.assertEquals("The edge is submitted once, without a second binding draw", 1, draws);
    }
}
