package xin.vanilla.banira.internal.fabric;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 约束 Fabric 在玩家离线时先派发保存，再派发登出。 */
public class FabricPlayerSaveEventContractTest {
    @Test
    public void disconnectSavesBeforeDispatchingLogout() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/BaniraCodex.java")), StandardCharsets.UTF_8);
        int disconnect = source.indexOf("ServerPlayConnectionEvents.DISCONNECT.register");
        int save = source.indexOf("BaniraEventBus.dispatchPlayerSave", disconnect);
        int logout = source.indexOf("BaniraEventBus.dispatchPlayerLoggedOut", disconnect);

        assertTrue(disconnect >= 0);
        assertTrue(save > disconnect);
        assertTrue(logout > save);
    }
}
