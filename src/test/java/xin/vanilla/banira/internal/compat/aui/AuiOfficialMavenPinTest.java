package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.Assert.*;

public class AuiOfficialMavenPinTest {
    private static final String SHA256 = "284C6697F492982752AF9F6AC045F2402D19BE601B1D3CD011AC463716C15925";

    private String contract() throws Exception {
        return Files.readString(Path.of("gradle/aui-contract.gradle"), StandardCharsets.UTF_8);
    }

    @Test
    public void officialCoordinateRemainsStrictAndExclusive() throws Exception {
        String script = contract();
        assertTrue(script.contains("repositories.exclusiveContent"));
        assertTrue(script.contains("https://maven.sighs.cc/repository/maven-public/"));
        assertTrue(script.contains("includeModule('com.sighs', 'ApricityUI-fabric-1.21.1')"));
        assertTrue(script.contains("auiContract('com.sighs:ApricityUI-fabric-1.21.1:1.3.0')"));
        assertTrue(script.contains("version { strictly('1.3.0') }"));
        assertFalse(script.contains("1.2.9"));
    }

    @Test
    public void localOverridesRetainAbsolutePathAndHashValidation() throws Exception {
        String script = contract();
        assertTrue(script.contains("!artifact.isAbsolute() || !artifact.isFile()"));
        assertTrue(script.contains("MessageDigest.getInstance('SHA-256')"));
        assertTrue(script.contains("if (!hash.equalsIgnoreCase('" + SHA256 + "'))"));
        assertTrue(script.contains("tasks.named('compileJava') { dependsOn verify }"));
        assertTrue(script.contains("systemProperty 'banira.aui.officialContractJar'"));
        assertTrue(script.contains("Expected one Loom-remapped AUI compile contract"));
    }

    @Test
    public void officialArchiveMatchesPinnedHash() throws Exception {
        Path input = Path.of(System.getProperty("banira.aui.officialContractJar"));
        assertTrue(input.isAbsolute());
        assertEquals(SHA256, HexFormat.of().withUpperCase().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(input))));
    }

    @Test
    public void officialArchiveIsNotOnBusinessTestRuntime() {
        assertNull(getClass().getClassLoader().getResource("com/sighs/apricityui/init/Document.class"));
    }
}
