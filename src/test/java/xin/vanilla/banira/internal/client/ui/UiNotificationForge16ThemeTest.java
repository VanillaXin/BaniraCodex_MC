package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class UiNotificationForge16ThemeTest {
    private ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void nativeNotificationLogUsesTheExistingForge16ThemeAccessors() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream(
                "/xin/vanilla/banira/client/gui/NotificationLogScreen.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        Set<String> getters = new HashSet<>();
        for (MethodNode method : node.methods)
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    assertFalse(call.desc.contains("BaniraColorToken"));
                    if (call.owner.equals("xin/vanilla/banira/client/data/BaniraColorConfig"))
                        getters.add(call.name);
                }
        for (String name : new String[]{"panelBg", "accent", "bgTertiary", "bgSecondary",
                "textHint", "textPrimary", "textSecondary"})
            assertTrue("Missing native theme accessor " + name, getters.contains(name));
    }

    @Test
    public void notificationHotkeysKeepTheirPublicLegacyEntryPoint() throws Exception {
        MethodNode entry = null;
        for (MethodNode method : read("client/gui/NotificationLogScreen").methods)
            if (method.name.equals("openHotkeyScreenIfPressed")) entry = method;
        assertNotNull(entry);
        assertEquals("()V", entry.desc);
        assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                entry.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
    }

    @Test
    public void richTextActionsUseTheMinecraftAndStyleSignature() throws Exception {
        int calls = 0;
        for (MethodNode method : read("client/gui/NotificationLogScreen").methods)
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.endsWith("/NotificationStyleInteractionHelper") && call.name.equals("tryClickStyle")) {
                        assertEquals("(Lnet/minecraft/client/Minecraft;Lnet/minecraft/util/text/Style;)Z", call.desc);
                        calls++;
                    }
                }
        assertEquals(1, calls);
    }

    @Test
    public void contextMenusRestoreTheForge16FixedFunctionState() throws Exception {
        Set<String> state = new HashSet<>();
        int restores = 0;
        for (MethodNode method : read("client/gui/quickaction/QuickActionOverlay").methods)
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.name.equals("restoreGuiRenderState")) {
                        assertTrue(call.owner.endsWith("/QuickActionOverlay"));
                        restores++;
                    }
                    if (method.name.equals("restoreGuiRenderState") && call.owner.endsWith("/RenderSystem"))
                        state.add(call.name);
                }
        assertEquals(2, restores);
        for (String name : new String[]{"enableTexture", "enableBlend", "defaultBlendFunc", "color4f"})
            assertTrue(state.contains(name));
    }
}
