package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraNetworkService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 锁定 1.0.2 已发布 API，并要求后续改动使用新的补丁版本。
 */
public class ReleasedApiCompatibilityTest {

    @Test
    public void releasedNetworkRegistrationSignaturesRemainAvailable() throws Exception {
        assertNotNull(BaniraNetworkService.class.getMethod(
                "registrar", String.class, BaniraIdentifier.class
        ));
        assertNotNull(BaniraNetworkService.class.getMethod(
                "registrar", String.class, BaniraIdentifier.class, String.class, boolean.class
        ));
        assertNotNull(BaniraModPresence.class.getMethod(
                "register", String.class, Consumer.class
        ));
    }

    @Test
    public void currentDevelopmentVersionIsOneZeroThree() throws Exception {
        String properties = new String(
                Files.readAllBytes(Paths.get("gradle.properties")),
                StandardCharsets.UTF_8
        );
        assertTrue(properties.contains("mod_version=1.0.3"));
    }

    @Test
    public void childModsCanQueryRemoteClientPresenceThroughStableApi() throws Exception {
        assertEquals(boolean.class, BaniraModPresence.class
                .getMethod("isRemoteClientInstalled", Object.class, String.class)
                .getReturnType());
    }
}
