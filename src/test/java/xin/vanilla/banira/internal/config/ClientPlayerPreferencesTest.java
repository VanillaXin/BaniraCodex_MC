package xin.vanilla.banira.internal.config;

import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.Assert.*;

public class ClientPlayerPreferencesTest {
    private static final String SELF = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER = "22222222-2222-2222-2222-222222222222";
    private final Object origin = new Object();
    private JsonObject server, client;

    @Before
    public void isolate() throws Exception {
        server = replace("customConfig", new JsonObject());
        client = replace("clientConfig", new JsonObject());
        CustomConfig.clearPlayerPreferencesClient();
    }

    @After
    public void restore() throws Exception {
        replace("customConfig", server);
        replace("clientConfig", client);
        CustomConfig.clearPlayerPreferencesClient();
        CustomConfig.setDirty(false);
    }

    private JsonObject replace(String name, JsonObject value) throws Exception {
        Field field = CustomConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        JsonObject previous = (JsonObject) field.get(null);
        field.set(null, value);
        return previous;
    }

    private Map<String, String> snapshot() {
        CustomConfig.setPlayerLanguage(SELF, "server");
        CustomConfig.setPlayerNotificationReceiveMode(SELF, "vanilla_message");
        return CustomConfig.playerPreferencesSnapshot(SELF);
    }

    @Test
    public void snapshotContainsOnlyRecipientsPreferences() {
        CustomConfig.setPlayerLanguage(OTHER, "zh_cn");
        Map<String, String> values = snapshot();
        assertEquals(com.google.common.collect.ImmutableMap.of("uuid", SELF, "language", "server",
                "notificationReceiveMode", "vanilla_message"), values);
        assertFalse(values.toString().contains(OTHER));
    }

    @Test
    public void existingSnapshotHandlerAppliesAuthoritativePreferenceCache() {
        assertTrue(CustomConfig.beginPlayerPreferencesClient(origin, SELF));
        assertTrue(CustomConfig.applyPlayerPreferencesClient(origin, SELF, snapshot()));
        assertEquals("server", CustomConfig.getPlayerLanguageClient(SELF));
        assertEquals("vanilla_message", CustomConfig.getPlayerNotificationReceiveModeClient(SELF));
        assertFalse(CustomConfig.applyPlayerPreferencesClient(new Object(), SELF, snapshot()));
        assertEquals("server", CustomConfig.getPlayerLanguageClient(SELF));
    }

    @Test
    public void missingAndDisconnectedPreferencesAreNotAuthoritative() {
        assertNull(CustomConfig.playerPreferencesClient(SELF));
        assertFalse(CustomConfig.applyPlayerPreferencesClient(origin, SELF, snapshot()));
        CustomConfig.beginPlayerPreferencesClient(origin, SELF);
        assertTrue(CustomConfig.applyPlayerPreferencesClient(origin, SELF, snapshot()));
        assertNotNull(CustomConfig.playerPreferencesClient(SELF));
        CustomConfig.clearPlayerPreferencesClient();
        assertNull(CustomConfig.playerPreferencesClient(SELF));
        assertEquals("client", CustomConfig.getPlayerLanguageClient(SELF));
    }

    @Test
    public void malformedSnapshotDoesNotMutateClientPreferences() {
        CustomConfig.beginPlayerPreferencesClient(origin, SELF);
        assertFalse(CustomConfig.applyPlayerPreferencesClient(origin, SELF,
                com.google.common.collect.ImmutableMap.of("uuid", "player.injected", "language", "server",
                        "notificationReceiveMode", "vanilla_message")));
        assertFalse(CustomConfig.applyPlayerPreferencesClient(origin, SELF,
                com.google.common.collect.ImmutableMap.of("uuid", SELF)));
        assertEquals("client", CustomConfig.getPlayerLanguageClient(SELF));
    }

    @Test
    public void loginSaveAndDisconnectPathsWireThePreferenceLifecycle() throws Exception {
        assertCalls("common/network/packet/ConfigFetchRequestToServer", "playerPreferencesSnapshot");
        assertCalls("common/network/packet/CustomPlayerConfigSyncToServer", "playerPreferencesSnapshot");
        assertCalls("internal/client/BaniraCodexClientBootstrap", "clearPlayerPreferencesClient");
    }

    @Test
    public void presenceHandshakeGatesTheClientSnapshotRequest() throws Exception {
        ClassNode node = node("internal/client/ClientPlayerPreferences");
        int check = -1, send = -1, index = 0;
        for (org.objectweb.asm.tree.MethodNode method : node.methods) {
            if (!method.name.equals("tick")) continue;
            for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.name.equals("isRemoteServerModInstalled")) check = index;
                    if (call.name.equals("sendPacketToServer")) send = index;
                }
                index++;
            }
        }
        assertTrue(check >= 0 && send > check);
    }

    private ClassNode node(String owner) throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/" + owner + ".class")) {
            assertNotNull(owner, input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private void assertCalls(String owner, String target) throws Exception {
        boolean found = false;
        for (org.objectweb.asm.tree.MethodNode method : node(owner).methods)
            for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("xin/vanilla/banira/internal/config/CustomConfig") && call.name.equals(target)) found = true;
                }
        assertTrue("Missing preference lifecycle wiring: " + owner, found);
    }
}
