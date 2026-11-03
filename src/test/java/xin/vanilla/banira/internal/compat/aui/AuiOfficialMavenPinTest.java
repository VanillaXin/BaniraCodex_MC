package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.Assert.*;

public class AuiOfficialMavenPinTest {
    private static final String SHA256 = "9F7A199D835EEC069E9C26D42AA8D65D2873ED219CA168750A69BD5408B42C53";

    private String contract() throws Exception {
        return Files.readString(Path.of("gradle/aui-contract.gradle"), StandardCharsets.UTF_8);
    }

    @Test
    public void officialCoordinateRemainsStrictAndExclusive() throws Exception {
        String script = contract();
        assertTrue(script.contains("repositories.exclusiveContent"));
        assertTrue(script.contains("https://maven.sighs.cc/repository/maven-public/"));
        assertTrue(script.contains("includeModule('com.sighs', 'ApricityUI-forge-1.19.2')"));
        assertTrue(script.contains("auiContract('com.sighs:ApricityUI-forge-1.19.2:1.3.0')"));
        assertTrue(script.contains("version { strictly('1.3.0') }"));
        assertFalse(script.contains("1.2.9"));
    }

    @Test
    public void localOverridesRetainAbsolutePathAndHashValidation() throws Exception {
        String script = contract();
        assertTrue(script.contains("!artifact.isAbsolute() || !artifact.isFile()"));
        assertTrue(script.contains("MessageDigest.getInstance('SHA-256')"));
        assertTrue(script.contains("if (!actual.equalsIgnoreCase('" + SHA256 + "'))"));
        assertTrue(script.contains("tasks.named('compileJava') { dependsOn verifyAuiContract }"));
        assertTrue(script.contains("dependsOn verifyAuiContract"));
    }

    @Test
    public void officialArchiveMatchesPinnedHash() throws Exception {
        Path input = Path.of(System.getProperty("banira.aui.contractJar"));
        assertTrue(input.isAbsolute());
        assertEquals(SHA256, HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input))));
    }

    @Test
    public void officialArchiveIsNotOnBusinessTestRuntime() {
        assertNull(getClass().getClassLoader().getResource("com/sighs/apricityui/init/Document.class"));
    }
}
