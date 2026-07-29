package xin.vanilla.banira.dependency;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalPublicationFingerprintContractTest {
    @Test
    public void mavenLocalPublicationWritesArtifactFingerprint() throws Exception {
        String buildScript = read("build.gradle");
        String fingerprintScript = read("gradle/publish-local-fingerprint.gradle");

        assertTrue(buildScript.contains("apply from: \"gradle/publish-local-fingerprint.gradle\""));
        assertTrue(fingerprintScript.contains("PublishToMavenLocal"));
        assertTrue(fingerprintScript.contains("local-build.json"));
        assertTrue(fingerprintScript.contains("SHA-256"));
        assertTrue(fingerprintScript.contains("publishMavenJavaPublicationToMavenLocal"));
        assertTrue(fingerprintScript.contains("findByName(\"MavenLocal\")"));
        assertFalse(fingerprintScript.contains("System.getProperty(\"user.home\")"));
    }

    private String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
