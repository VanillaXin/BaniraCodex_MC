package xin.vanilla.banira.internal.fabric.network;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Fabric 的公开模组存在性查询必须读取 Banira 握手状态。 */
public class FabricModPresenceContractTest {
    @Test
    public void networkServiceExposesRemoteClientPresence() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricBaniraNetworkService.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("boolean isRemoteClientModInstalled"));
        assertTrue(source.contains("PlayerUtils.isRemoteClientModInstalled"));
    }
}
