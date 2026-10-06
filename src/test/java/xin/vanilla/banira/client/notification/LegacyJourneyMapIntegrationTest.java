package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class LegacyJourneyMapIntegrationTest {

    @Test
    public void nativeSlotReadsUsePositiveClockGranularity() throws Exception {
        ClassNode node = read("/xin/vanilla/banira/internal/compat/minimap/LegacyJourneyMapNotifications.class");
        java.util.List<Long> clock = new java.util.ArrayList<>();
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<clinit>")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() == org.objectweb.asm.Opcodes.LCONST_0) clock.add(0L);
                if (instruction.getOpcode() == org.objectweb.asm.Opcodes.LCONST_1) clock.add(1L);
                if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof Long)
                    clock.add((Long) ((LdcInsnNode) instruction).cst);
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("create")) break;
            }
        }
        assertEquals(2, clock.size());
        journeymap.client.ui.theme.ThemeLabelSource.InfoSlot slot =
                new journeymap.client.ui.theme.ThemeLabelSource.InfoSlot(
                        "banira_codex", "test", clock.get(0), clock.get(1), () -> "Unread 23");
        assertEquals("Unread 23", slot.getLabelText(123456L));
    }
    private ClassNode read(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(path, input);
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, 0);
            return result;
        }
    }

    @Test
    public void visibilityGuardUsesNativeMapStateAndHudVisibility() throws Exception {
        ClassNode node = read("/xin/vanilla/banira/internal/compat/minimap/LegacyJourneyMapNotifications.class");
        Set<String> calls = new HashSet<>();
        for (MethodNode method : node.methods) {
            if (!method.name.equals("canUseMap")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    calls.add(call.owner + "." + call.name);
                }
            }
        }
        assertTrue(calls.contains("journeymap/client/ui/UIManager.isMiniMapEnabled"));
        assertTrue(calls.contains("journeymap/client/ui/minimap/MiniMap.uiState"));
        assertTrue(calls.contains("xin/vanilla/banira/client/notification/NotificationUnreadHud.isVisible"));
    }

    @Test
    public void constructorMeasuresAndPositionsTheSameBottomSlots() throws Exception {
        ClassNode node = read("/xin/vanilla/banira/internal/mixin/compat/minimap/JourneyMapInfoSlotsMixin.class");
        Set<Integer> indexes = new HashSet<>();
        int callbacks = 0;
        for (MethodNode method : node.methods) {
            if (method.visibleAnnotations == null) continue;
            for (AnnotationNode annotation : method.visibleAnnotations) {
                if (!annotation.desc.endsWith("/ModifyArg;")) continue;
                callbacks++;
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    if ("index".equals(annotation.values.get(i))) indexes.add((Integer) annotation.values.get(i + 1));
                }
                assertTrue(method.instructions.iterator().hasNext());
                boolean shared = false;
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode) instruction;
                        shared |= call.owner.endsWith("/LegacyJourneyMapNotifications") && call.name.equals("append");
                    }
                }
                assertTrue("Measurement and positioning must share slot selection", shared);
            }
        }
        assertEquals(2, callbacks);
        assertTrue(indexes.contains(2));
        assertTrue(indexes.contains(4));
    }
}
