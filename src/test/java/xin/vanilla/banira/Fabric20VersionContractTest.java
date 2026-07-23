package xin.vanilla.banira;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 锁定 Fabric 1.20.1 的构建版本与可解析仓库。 */
public class Fabric20VersionContractTest {
    @Test
    public void buildUsesFabric20VersionsAndReleaseRepository() throws Exception {
        String build = read("build.gradle");
        String properties = read("gradle.properties");
        String resourcePack = read("src/main/resources/pack.mcmeta");

        assertTrue(properties.contains("minecraft_version=1.20.1"));
        assertTrue(resourcePack.contains("\"pack_format\": 15"));
        assertTrue(properties.contains("loader_version=0.16.10"));
        assertTrue(properties.contains("fabric_version=0.92.9+1.20.1"));
        assertTrue(properties.contains("modmenu_version=7.2.2"));
        assertTrue(build.contains("https://maven.terraformersmc.com/releases/"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
