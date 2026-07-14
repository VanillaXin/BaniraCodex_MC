package xin.vanilla.banira.contract;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 保证子 mod 从 Maven Local 获取 Loom remap 后的正式主 JAR。 */
public class MavenPublicationContractTest {
    @Test
    public void mavenPublicationUsesUnclassifiedRemapJar() throws Exception {
        String repository = System.getProperty("banira.publication.repository");
        String group = System.getProperty("banira.publication.group");
        String artifact = System.getProperty("banira.publication.artifact");
        String version = System.getProperty("banira.publication.version");
        String remapJar = System.getProperty("banira.publication.remapJar");
        assertNotNull("Publication contract repository is not configured", repository);
        assertNotNull(group);
        assertNotNull(artifact);
        assertNotNull(version);
        assertNotNull(remapJar);

        Path versionDir = Paths.get(repository)
                .resolve(group.replace('.', '/'))
                .resolve(artifact)
                .resolve(version);
        Path mainJar = versionDir.resolve(artifact + "-" + version + ".jar");
        Path sourcesJar = versionDir.resolve(artifact + "-" + version + "-sources.jar");
        Path devJar = versionDir.resolve(artifact + "-" + version + "-dev.jar");

        assertTrue("Unclassified Maven main jar is missing", Files.isRegularFile(mainJar));
        assertTrue("Maven sources jar is missing", Files.isRegularFile(sourcesJar));
        assertFalse("Development jar must not be published by the contract repository", Files.exists(devJar));
        assertEquals("Maven main jar differs from Loom remapJar",
                -1L, Files.mismatch(mainJar, Paths.get(remapJar)));

        try (ZipFile zip = new ZipFile(mainJar.toFile(), StandardCharsets.UTF_8)) {
            assertNotNull("Published jar is missing fabric.mod.json", zip.getEntry("fabric.mod.json"));
            assertNotNull("Published jar is missing Banira entry class",
                    zip.getEntry("xin/vanilla/banira/BaniraCodex.class"));
        }
    }
}
