package xin.vanilla.banira.internal.forge.platform;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Forge 平台主体在专用服务器加载时不得直接解析客户端实现类。
 */
public class ForgePlatformDedicatedServerBoundaryTest {
    @Test
    public void clientServicesAreIsolatedBehindALazyNestedBridge() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/forge/platform/ForgeBaniraPlatform.java"
        )), StandardCharsets.UTF_8);

        assertFalse(source.contains("import xin.vanilla.banira.internal.client."));
        assertFalse(source.contains("import xin.vanilla.banira.internal.forge.client."));
        assertTrue(source.contains("private static final class ClientServices"));
        assertTrue(source.contains("FMLEnvironment.dist == Dist.CLIENT"));
    }

    @Test
    public void loaderEntryDoesNotLinkTheClientBootstrapDirectly() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/forge/ForgeBaniraCodexEntry.java"
        )), StandardCharsets.UTF_8);

        assertFalse(source.contains("xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap::init"));
        assertTrue(source.contains("ForgeBaniraClientBootstrap::init"));
    }
}
