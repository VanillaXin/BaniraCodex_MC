package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.Assert.*;

public class UiPlayerPreferenceNetworkWiringTest {
    private ClassNode source(String path) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + path + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private boolean calls(ClassNode node, String ownerSuffix, String name) {
        for (MethodNode method : node.methods)
            for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.endsWith(ownerSuffix) && call.name.equals(name)) return true;
                }
        return false;
    }

    @Test
    public void requestIsReadOnlyAndRestrictedToTheContextSender() throws Exception {
        ClassNode fetch = source("common/network/packet/ConfigFetchRequestToServer");
        assertTrue(calls(fetch, "ServerSenderAccess", "uuidString"));
        assertTrue(calls(fetch, "CustomConfig", "playerPreferencesSnapshot"));
        assertFalse(calls(fetch, "CustomConfig", "setPlayerLanguage"));
        assertFalse(calls(fetch, "CustomConfig", "setPlayerNotificationReceiveMode"));
        assertTrue(calls(fetch, "ServerSenderAccess", "canAccessServerConfigEditor"));
    }

    @Test
    public void snapshotCapturesTransportIdentityBeforeQueuedDispatch() throws Exception {
        ClassNode packet = source("common/network/packet/ConfigSnapshotToClient");
        MethodNode handle = packet.methods.stream().filter(m -> m.name.equals("handle")).findFirst().get();
        boolean captured = false;
        for (AbstractInsnNode instruction : handle.instructions)
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.name.equals("connectionKey")) captured = true;
                if (call.name.equals("enqueueWork")) assertTrue(captured);
            }
        assertTrue(captured);
        assertTrue(calls(packet, "BaniraClientPacketDispatch", "handlePlayerPreferences"));
    }

    @Test
    public void preferenceFetchUsesTheExistingEndTickEventApi() throws Exception {
        ClassNode defaults = source("internal/client/BaniraClientNetworkDefaults");
        assertTrue(calls(defaults, "BaniraClientEventHub$Client", "onClientTick"));
        assertFalse(calls(defaults, "BaniraClientEventHub$Client", "onClientTickEnd"));
        int ticks = 0;
        boolean endPhase = false;
        for (MethodNode method : defaults.methods)
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (field.owner.endsWith("/BaniraClientTickEvent") && field.name.equals("END"))
                        endPhase = true;
                }
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.endsWith("/ClientPlayerPreferences") && call.name.equals("tick")) ticks++;
                }
            }
        assertTrue(endPhase);
        assertEquals(1, ticks);
    }

    @Test
    public void saveReturnsSnapshotAndClientLogoutClearsIt() throws Exception {
        assertTrue(calls(source("common/network/packet/CustomPlayerConfigSyncToServer"), "CustomConfig", "playerPreferencesSnapshot"));
        assertTrue(calls(source("internal/client/BaniraClientNetworkDefaults"), "BaniraClientPacketDispatch", "registerPlayerPreferencesHandler"));
        assertTrue(calls(source("internal/client/ClientPlayerPreferences"), "CustomConfig", "clearPlayerPreferencesClient"));
    }
}
