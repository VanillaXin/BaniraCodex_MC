package xin.vanilla.banira.internal.fabric;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class FabricExternalInventoryCompatibilityContractTest {
    @Test
    public void clientInstallsOptionalBridgeAfterBaniraSetup() throws Exception {
        String client = source("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java");
        int setup = client.indexOf("BaniraClientModSetup.initOnClientSetup()");
        int bridge = client.indexOf("FabricExternalInventoryCompatibility.init()");
        assertTrue(setup >= 0 && bridge > setup);

        String mixins = source("src/main/resources/banira_codex.mixins.json");
        assertTrue(mixins.contains("FabricMixinConfigPlugin"));
        assertTrue(mixins.contains("compat.ftblibrary.SidebarButtonMixin"));
        assertTrue(mixins.contains("compat.ftblibrary.SidebarGroupGuiButtonMixin"));
    }

    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
