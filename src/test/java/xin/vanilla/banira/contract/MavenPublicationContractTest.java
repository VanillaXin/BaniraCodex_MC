package xin.vanilla.banira.contract;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 保证子 mod 从 Maven Local 获取 Loom remap 后的正式主 JAR。 */
public class MavenPublicationContractTest {
    @Test
    public void mavenPublicationUsesUnclassifiedRemapJar() throws Exception {
        Path buildFile = Paths.get("build.gradle");
        String script = new String(Files.readAllBytes(buildFile), StandardCharsets.UTF_8);

        assertTrue("Maven publication must use remapJar",
                script.contains("artifact tasks.named('remapJar')"));
        assertFalse("Development jar must not be the Maven main artifact",
                script.contains("artifact tasks.jar"));
        assertTrue("Every PublishToMavenLocal task must build remapped artifacts first",
                script.contains("tasks.withType(PublishToMavenLocal).configureEach"));
    }
}
