package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class AuiOfficialReferenceTest {
    private static final String SHA256 = "37D5790453D0F28827555B3C6F9FE31328B6829EC4E07E6A4718365E9A5CFB6C";

    private Path official() {
        return Paths.get(System.getProperty("banira.aui.contractJar"));
    }

    @Test
    public void officialReferenceUsesStrictNativeForge16Coordinates() throws Exception {
        String script = new String(Files.readAllBytes(Paths.get("gradle/aui-contract.gradle")), StandardCharsets.UTF_8);
        assertTrue(script.contains("https://maven.sighs.cc/repository/maven-public/"));
        assertTrue(script.contains("includeModule('com.sighs', 'ApricityUI-forge-1.16.5')"));
        assertTrue(script.contains("'com.sighs:ApricityUI-forge-1.16.5:1.3.0'"));
        assertTrue(script.contains("strictly('1.3.0')"));
        assertTrue(script.contains("def referenceHash = '" + SHA256 + "'"));
        assertTrue(script.contains("Pinned AUI declaration reference SHA-256 mismatch"));
        assertFalse(script.contains("ApricityUI-forge-1.18.2"));
        assertFalse(script.contains("com/mojang/blaze3d/vertex/PoseStack"));
    }

    @Test
    public void rawOfficialBytesAndJava17HeaderRemainPinned() throws Exception {
        assertTrue(official().isAbsolute());
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(official()));
        StringBuilder actual = new StringBuilder();
        for (byte value : bytes) actual.append(String.format(Locale.ROOT, "%02X", value & 255));
        assertEquals(SHA256, actual.toString());
        try (JarFile jar = new JarFile(official().toFile());
             DataInputStream input = new DataInputStream(jar.getInputStream(jar.getJarEntry("com/sighs/apricityui/init/Document.class")))) {
            assertEquals(0xCAFEBABE, input.readInt());
            assertEquals(0, input.readUnsignedShort());
            assertEquals(61, input.readUnsignedShort());
        }
    }

    @Test
    public void actualHostSelectsTheOfficialContractWithoutLoadingEngineClasses() throws Exception {
        try (final URLClassLoader jar = new URLClassLoader(new URL[]{official().toUri().toURL()}, null)) {
            ClassLoader resources = new ClassLoader(null) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) {
                    throw new AssertionError("Optional class loading during resource inspection: " + name);
                }

                @Override
                public InputStream getResourceAsStream(String name) {
                    return jar.getResourceAsStream(name);
                }
            };
            assertFalse(AuiCapabilityContract.inspect(resources, 52).compatible());
            assertTrue(AuiCapabilityContract.inspect(resources, 61).mismatch(), AuiCapabilityContract.inspect(resources, 61).compatible());
            boolean capable = AuiCapabilityContract.maximumClassVersion(System.getProperty("java.class.version")) >= 61;
            assertEquals(capable, AuiCapabilityContract.inspect(resources).compatible());
        }
    }

    @Test
    public void dedicatedHostMatchesRequestedJvmWithoutChangingProductionTarget() throws Exception {
        String expected = System.getProperty("banira.aui.expectedHostMajor");
        if (expected != null) assertEquals(expected, System.getProperty("java.class.version"));
        else assertEquals("52.0", System.getProperty("java.class.version"));
        String build = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8);
        assertTrue(build.contains("def javaVer = 8"));
    }
}
