package xin.vanilla.banira.client.notification;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class NotificationMinimapScreenVisibilityTest {
    private boolean allows(boolean minimap, boolean screenOpen, boolean chat) throws Exception {
        Method method = null;
        for (Method candidate : NotificationHudState.class.getDeclaredMethods()) {
            if (candidate.getName().equals("allowsScreen")) method = candidate;
        }
        assertNotNull("Separate minimap scene policy", method);
        return (Boolean) method.invoke(null, minimap, screenOpen, chat);
    }

    @Test
    public void visibleMinimapKeepsTextAcrossInventoryAndChat() throws Exception {
        assertTrue(allows(true, false, false));
        assertTrue(allows(true, true, false));
        assertTrue(allows(true, true, true));
    }

    @Test
    public void standaloneKeepsExistingScreenRestrictions() throws Exception {
        assertTrue(allows(false, false, false));
        assertTrue(allows(false, true, true));
        assertFalse(allows(false, true, false));
    }

    @Test
    public void screenInputDoesNotChangeToggleAndOffStillHides() {
        NotificationHudState state = new NotificationHudState();
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, true, true, true);
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, false, true, false);
        assertTrue(state.visible(0));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, true, true, false);
        assertTrue(state.visible(8));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.OFF, false, true, false);
        assertFalse(state.visible(8));
    }

    private boolean calls(String name, String method, String target) throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        for (MethodNode m : node.methods) {
            if (!m.name.equals(method)) continue;
            for (AbstractInsnNode instruction : m.instructions) {
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals(target))
                    return true;
            }
        }
        return false;
    }

    @Test
    public void minimapCallbackUsesMapVisibility() throws Exception {
        assertTrue(calls("client/notification/NotificationMinimapBridge", "claim", "isMinimapVisible"));
    }

    @Test
    public void journeyLayoutUsesMapVisibility() throws Exception {
        assertTrue(calls("internal/compat/minimap/LegacyJourneyMapNotifications", "refreshLayout", "isMinimapVisible"));
    }

    private boolean mapVisible(NotificationHudState state) throws Exception {
        Method method = null;
        for (Method candidate : NotificationHudState.class.getDeclaredMethods()) {
            if (candidate.getName().equals("visibleOnMinimap")) method = candidate;
        }
        assertNotNull("Native visibility must not depend on window focus", method);
        return (Boolean) method.invoke(state, 1);
    }

    @Test
    public void nativeAlwaysSurvivesFocusLossWhileStandaloneHides() throws Exception {
        NotificationHudState state = new NotificationHudState();
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.ALWAYS, false, true, true);
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.ALWAYS, false, false, false);
        assertFalse(state.visible(1));
        assertTrue(mapVisible(state));
    }

    @Test
    public void nativeToggleSurvivesFocusLossWithoutAcceptingNewKeys() throws Exception {
        NotificationHudState state = new NotificationHudState();
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, true, true, true);
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, false, false, false);
        assertTrue(mapVisible(state));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, true, false, false);
        assertTrue(mapVisible(state));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, false, true, true);
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.TOGGLE, true, true, true);
        assertFalse(mapVisible(state));
    }

    @Test
    public void nativeHoldOffAndWorldExitKeepTheirVisibilityRules() throws Exception {
        NotificationHudState state = new NotificationHudState();
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.HOLD, true, true, true);
        assertTrue(mapVisible(state));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.HOLD, false, false, false);
        assertFalse(mapVisible(state));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.OFF, true, true, true);
        assertFalse(mapVisible(state));
        state.update(xin.vanilla.banira.common.enums.EnumNotificationHudMode.ALWAYS, false, true, true);
        state.reset();
        assertFalse(mapVisible(state));
    }
}
