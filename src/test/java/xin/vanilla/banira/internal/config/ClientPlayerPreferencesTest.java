package xin.vanilla.banira.internal.config;

import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.network.packet.ConfigSnapshotToClient;
import xin.vanilla.banira.internal.client.BaniraClientPacketHandlers;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.*;

public class ClientPlayerPreferencesTest {
    private static final String SELF = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER = "22222222-2222-2222-2222-222222222222";
    private JsonObject server, client;

    @Before
    public void isolate() throws Exception {
        server = replace("customConfig", new JsonObject());
        client = replace("clientConfig", new JsonObject());
    }

    @After
    public void restore() throws Exception {
        replace("customConfig", server);
        replace("clientConfig", client);
        CustomConfig.setDirty(false);
    }

    private static JsonObject replace(String name, JsonObject value) throws Exception {
        Field field = CustomConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        JsonObject previous = (JsonObject) field.get(null);
        field.set(null, value);
        return previous;
    }

    private static Object call(String name, Class<?>[] types, Object... args) throws Exception {
        try {
            return CustomConfig.class.getMethod(name, types).invoke(null, args);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Missing own-player preference synchronization: " + name, missing);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> snapshot() throws Exception {
        return (Map<String, String>) call("playerPreferencesSnapshot", new Class<?>[]{String.class}, SELF);
    }

    @Test
    public void snapshotContainsOnlyRecipientsPreferences() throws Exception {
        CustomConfig.setPlayerLanguage(SELF, "server");
        CustomConfig.setPlayerNotificationReceiveMode(SELF, "vanilla_message");
        CustomConfig.setPlayerLanguage(OTHER, "zh_cn");
        Map<String, String> snapshot = snapshot();
        assertEquals(Map.of("uuid", SELF, "language", "server", "notificationReceiveMode", "vanilla_message"), snapshot);
        assertFalse(snapshot.toString().contains(OTHER));
    }

    @Test
    public void existingSnapshotHandlerAppliesAuthoritativePreferenceCache() throws Exception {
        CustomConfig.setPlayerLanguage(SELF, "server");
        CustomConfig.setPlayerNotificationReceiveMode(SELF, "vanilla_message");
        String name;
        try {
            name = (String) CustomConfig.class.getField("PLAYER_PREFERENCES_CONFIG").get(null);
        } catch (NoSuchFieldException missing) {
            throw new AssertionError("Own-player snapshot route missing", missing);
        }
        BaniraClientPacketHandlers.applyConfigSnapshot(new ConfigSnapshotToClient(name, snapshot()));
        assertEquals("server", CustomConfig.getPlayerLanguageClient(SELF));
        assertEquals("vanilla_message", CustomConfig.getPlayerNotificationReceiveModeClient(SELF));
    }

    @Test
    public void missingAndDisconnectedPreferencesAreNotAuthoritative() throws Exception {
        assertNull(call("playerPreferencesClient", new Class<?>[]{String.class}, SELF));
        assertEquals(true, call("applyPlayerPreferencesClient", new Class<?>[]{Map.class},
                Map.of("uuid", SELF, "language", "server", "notificationReceiveMode", "vanilla_message")));
        assertNotNull(call("playerPreferencesClient", new Class<?>[]{String.class}, SELF));
        call("clearPlayerPreferencesClient", new Class<?>[]{});
        assertNull(call("playerPreferencesClient", new Class<?>[]{String.class}, SELF));
        assertEquals("client", CustomConfig.getPlayerLanguageClient(SELF));
    }

    @Test
    public void malformedSnapshotDoesNotMutateClientPreferences() throws Exception {
        assertEquals(false, call("applyPlayerPreferencesClient", new Class<?>[]{Map.class},
                Map.of("uuid", "player.injected", "language", "server", "notificationReceiveMode", "vanilla_message")));
        assertEquals(false, call("applyPlayerPreferencesClient", new Class<?>[]{Map.class}, Map.of("uuid", SELF)));
        assertEquals("client", CustomConfig.getPlayerLanguageClient(SELF));
    }

    @Test
    public void loginSaveAndDisconnectPathsWireThePreferenceLifecycle() throws Exception {
        assertCalls("xin/vanilla/banira/internal/common/BaniraCodexRuntime", "playerPreferencesSnapshot");
        assertCalls("xin/vanilla/banira/common/network/packet/CustomPlayerConfigSyncToServer", "playerPreferencesSnapshot");
        assertCalls("xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap", "clearPlayerPreferencesClient");
    }

    @Test
    public void presenceHandshakeRegistersAServerSyncCallback() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/common/BaniraCodexRuntime.class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            boolean found = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods)
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions)
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode invocation
                            && invocation.owner.equals("xin/vanilla/banira/common/network/ModLoadedPresenceStore")
                            && invocation.name.equals("register") && org.objectweb.asm.Type.getArgumentTypes(invocation.desc).length == 2)
                        found = true;
            assertTrue("Initial preferences must wait for the mod-presence handshake", found);
        }
    }

    private void assertCalls(String owner, String target) throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/" + owner + ".class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            boolean found = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods)
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions)
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode invocation
                            && invocation.owner.equals("xin/vanilla/banira/internal/config/CustomConfig") && invocation.name.equals(target))
                        found = true;
            assertTrue("Missing preference lifecycle wiring: " + owner, found);
        }
    }
}
