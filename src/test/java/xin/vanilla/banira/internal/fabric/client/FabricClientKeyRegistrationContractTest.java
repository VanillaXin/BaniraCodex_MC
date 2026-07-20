package xin.vanilla.banira.internal.fabric.client;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 锁定 Fabric 1.16.5 客户端入口会提交稳定输入 API 的待注册键位。 */
public class FabricClientKeyRegistrationContractTest {
    @Test
    public void clientEntrypointFlushesPublicInputQueue() throws Exception {
        Path path = Paths.get("src/main/java/xin/vanilla/banira/internal/fabric/client/FabricBaniraCodexClient.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        int entrypoint = source.indexOf("void onInitializeClient()");
        int flush = source.indexOf("BaniraInput.flushPendingRegistrations()", entrypoint);
        assertTrue(flush > entrypoint);
    }
}
