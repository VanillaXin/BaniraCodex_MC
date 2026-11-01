package xin.vanilla.banira.internal.client.ui;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric20PlayerConfigPortTest {
    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private long calls(ClassNode node, String method, String owner, String name) {
        return node.methods.stream().filter(candidate -> method == null || candidate.name.equals(method))
                .flatMap(candidate -> Arrays.stream(candidate.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(owner) && call.name.equals(name)).count();
    }

    @Test
    public void sharedNativeBaseAndPersonalFieldsKeepTheSessionOwner() throws Exception {
        ClassNode base = node("client/gui/PlayerConfigScreen");
        assertTrue(base.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
        assertTrue(calls(base, "onClose", "xin/vanilla/banira/internal/client/ui/CommonUiPages", "close") > 0);
        assertTrue(calls(base, "onRemoved", "xin/vanilla/banira/internal/client/ui/CommonUiPages", "removed") > 0);
        ClassNode personal = node("client/gui/CustomPlayerConfigEditScreen");
        assertTrue(calls(personal, "playerUiSession", "xin/vanilla/banira/internal/client/ui/page/PlayerConfigUiSession", "<init>") > 0);
        assertTrue(calls(personal, "persistPlayerConfig", "xin/vanilla/banira/common/util/PacketUtils", "sendPacketToServer") > 0);
        assertEquals(0, calls(personal, null, "xin/vanilla/banira/internal/config/CustomConfig", "setPlayerLanguageClient"));
        assertEquals(0, calls(personal, null, "xin/vanilla/banira/internal/config/CustomConfig", "setPlayerNotificationReceiveModeClient"));
    }

    @Test
    public void snapshotFenceUsesTheActualFabricTransportConnection() throws Exception {
        ClassNode transport = node("internal/fabric/network/FabricNetworkHandler");
        assertTrue(calls(transport, null, "xin/vanilla/banira/internal/fabric/network/FabricNetworkContext", "client") > 0);
        ClassNode runtime = node("internal/client/BaniraClientRuntime");
        assertTrue(calls(runtime, "isCurrentConnection", "net/minecraft/client/multiplayer/ClientPacketListener", "getConnection") > 0);
        assertTrue(calls(runtime, "isCurrentConnection", "net/minecraft/network/Connection", "isConnected") > 0);
        assertTrue(calls(node("internal/client/BaniraClientPacketHandlers"), "isCurrentConfigConnection",
                "xin/vanilla/banira/internal/client/BaniraClientRuntime", "isCurrentConnection") > 0);
    }

    @Test
    public void preferenceAndSnapshotPacketsKeepTheirExistingWirePrimitives() throws Exception {
        ClassNode save = node("common/network/packet/CustomPlayerConfigSyncToServer");
        assertEquals(2, calls(save, "toBytes", "xin/vanilla/banira/common/network/BaniraPacketBuffer", "writeUtf"));
        assertEquals(0, calls(save, "toBytes", "xin/vanilla/banira/common/network/BaniraPacketBuffer", "writeVarInt"));
        ClassNode snapshot = node("common/network/packet/ConfigSnapshotToClient");
        assertEquals(3, calls(snapshot, "toBytes", "xin/vanilla/banira/common/network/BaniraPacketBuffer", "writeUtf"));
        assertEquals(1, calls(snapshot, "toBytes", "xin/vanilla/banira/common/network/BaniraPacketBuffer", "writeVarInt"));
    }

    @Test
    public void bothDisconnectEntrancesUseTheFinallyProtectedCacheCleanup() throws Exception {
        ClassNode bootstrap = node("internal/client/BaniraCodexClientBootstrap");
        assertEquals(2, calls(bootstrap, null, "xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap", "disconnectUiPages"));
        assertTrue(calls(bootstrap, "disconnectUiPages", "xin/vanilla/banira/internal/client/ui/CommonUiPages", "disconnect") > 0);
        assertTrue(calls(bootstrap, "disconnectUiPages", "xin/vanilla/banira/internal/config/CustomConfig", "clearPlayerPreferencesClient") > 0);
        assertTrue(bootstrap.methods.stream().filter(method -> method.name.equals("disconnectUiPages"))
                .anyMatch(method -> method.tryCatchBlocks.stream().anyMatch(block -> block.type == null)));
        assertEquals(2, calls(bootstrap, "init", "xin/vanilla/banira/internal/client/ui/SharedHudHosts", "register"));
    }

    @Test
    public void personalScopeLabelIsPresentInBothLanguages() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            try (var input = getClass().getResourceAsStream("/assets/banira_codex/lang/" + language + ".json")) {
                assertNotNull(language, input);
                var values = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(values.has("word.banira_codex.player_config_personal"));
                assertFalse(values.get("word.banira_codex.player_config_personal").getAsString().isBlank());
            }
        }
    }
}
