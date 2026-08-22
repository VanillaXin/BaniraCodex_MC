package xin.vanilla.banira.internal.fabric;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class FabricExternalInventoryCompatibilityContractTest {
    @Test
    public void clientRegistersBaniraBeforeRefreshingOptionalBridges() throws Exception {
        String client = source("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java");
        int bootstrap = client.indexOf("BaniraCodexClientBootstrap.init()");
        int setup = client.indexOf("dispatchModClientSetup");
        int bridge = client.indexOf("FabricExternalInventoryCompatibility.init()");
        assertTrue(bootstrap >= 0 && setup > bootstrap && bridge > setup);

        String mixins = source("src/main/resources/banira_codex.mixins.json");
        assertTrue(mixins.contains("FabricMixinConfigPlugin"));
        assertTrue(mixins.contains("compat.ftblibrary.SidebarButtonMixin"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
