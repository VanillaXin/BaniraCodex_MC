package xin.vanilla.banira.internal.forge.config;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定旧版 Forge 配置必须绑定到当前正在构造的 mod 容器。 */
public class ForgeConfigRegistrationContractTest {
    @Test
    public void registrationUsesActiveContainerAndNeverFailsSilently() throws IOException {
        Path path = Paths.get("src/main/java/xin/vanilla/banira/internal/forge/config/ForgeConfigAdapter.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        assertTrue(source.contains("ModLoadingContext.get().getActiveContainer()"));
        assertTrue(source.contains("Config container is unavailable"));
        assertFalse(source.contains("getModContainerById(modId).ifPresent"));
    }
}
