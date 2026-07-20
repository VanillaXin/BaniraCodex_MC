package xin.vanilla.banira.common.config;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定 NeoForge 配置注册失败时必须在保存前给出明确错误。 */
public class ForgeConfigRegistrationContractTest {
    @Test
    public void registrationNeverSkipsAnUnavailableContainer() throws IOException {
        Path path = Path.of("src/main/java/xin/vanilla/banira/common/config/ForgeConfigAdapter.java");
        String source = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue(source.contains("Config container is unavailable"));
        assertFalse(source.contains("getModContainerById(modId).ifPresent"));
    }
}
