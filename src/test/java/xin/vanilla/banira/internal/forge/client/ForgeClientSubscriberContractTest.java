package xin.vanilla.banira.internal.forge.client;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Forge 52 的自动订阅器由 Dist 参数分侧，不能再叠加 OnlyIn。 */
public class ForgeClientSubscriberContractTest {
    @Test
    public void clientSubscriberUsesEventBusDistWithoutOnlyIn() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/forge/client/BaniraClientForgeEventHandler.java"
        )), StandardCharsets.UTF_8);

        assertTrue(source.contains("value = Dist.CLIENT"));
        assertFalse(source.contains("@OnlyIn"));
    }
}
