package xin.vanilla.banira.internal.client.ui;

import com.google.gson.JsonParser;
import org.junit.Test;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.Assert.*;

public class AuiLegacyRuntimeLibraryTest {
    private static final String JAR = "META-INF/jarjar/joml-1.10.5.jar";
    private static final String SHA256 = "cac9f22f83a7aa33eebda73c16ff5261e3cb4911b6bafcf4c79ea486099d0c9a";

    @Test
    public void developmentRuntimeContainsTheLegacyEngineLibrary() throws Exception {
        String build = Files.readString(Path.of("gradle/aui-contract.gradle"));
        assertTrue(build, build.contains("minecraftLibrary 'org.joml:joml:1.10.5'"));
    }

    @Test
    public void productionUsesNegotiatedNestedLibraryWithoutEmbeddingTheEngine() throws Exception {
        String build = Files.readString(Path.of("gradle/aui-contract.gradle"));
        assertTrue(build, build.contains("jarJar.enable()"));
        assertTrue(build, build.contains("jarJar.pin(it, '1.10.5')"));
        assertTrue(build, build.contains("include(name: 'META-INF/jarjar/**')"));
        assertFalse(build, build.contains("fromRuntimeConfiguration"));
    }

    @Test
    public void arbitraryNestedJarsDoNotReceiveTheRuntimeLibraryExemption() {
        assertFalse(isRuntimeLibrary("META-INF/jarjar/engine.jar", new byte[0]));
        assertFalse(isRuntimeLibrary(JAR, new byte[0]));
        assertFalse(UiArtifactBoundaryTest.violations(Map.of(JAR, new byte[0]), false).isEmpty());
    }


    static void verifyFormalRefmap(Map<String, byte[]> files) {
        assertNotNull("Missing generated Mixin refmap", files.get("banira_codex.refmap.json"));
        var refmap = JsonParser.parseString(new String(files.get("banira_codex.refmap.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        var mappings = refmap.getAsJsonObject("mappings");
        assertTrue(mappings.size() >= 5);
        var player = mappings.getAsJsonObject("xin/vanilla/banira/internal/mixin/accessors/ServerPlayerAccessor");
        assertTrue(player.get("chatVisibility").getAsString().startsWith("f_8922_:"));
        assertTrue(player.get("canChatColor").getAsString().startsWith("f_8923_:"));
        assertTrue(player.get("allowsListing").getAsString().startsWith("f_184127_:"));
        assertFalse(player.has("language"));
    }

    static boolean isRuntimeLibrary(String name, byte[] bytes) {
        try {
            return JAR.equals(name) && SHA256.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (java.security.NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    static void verifyFormalLibrary(Map<String, byte[]> files) throws Exception {
        assertNotNull("Missing Forge library metadata", files.get("META-INF/jarjar/metadata.json"));
        var metadata = JsonParser.parseString(new String(files.get("META-INF/jarjar/metadata.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        var jars = metadata.getAsJsonArray("jars");
        assertEquals(1, jars.size());
        var entry = jars.get(0).getAsJsonObject();
        assertEquals("org.joml", entry.getAsJsonObject("identifier").get("group").getAsString());
        assertEquals("joml", entry.getAsJsonObject("identifier").get("artifact").getAsString());
        assertEquals("1.10.5", entry.getAsJsonObject("version").get("artifactVersion").getAsString());
        assertEquals("[1.10.5,1.11)", entry.getAsJsonObject("version").get("range").getAsString());
        assertEquals(JAR, entry.get("path").getAsString());
        assertTrue("Runtime library identity changed", isRuntimeLibrary(JAR, files.getOrDefault(JAR, new byte[0])));
        assertFalse(files.keySet().stream().anyMatch(name -> name.startsWith("org/joml/")));
        Path library = Files.createTempFile("banira-joml-contract-", ".jar");
        try {
            Files.write(library, files.get(JAR));
            try (var loader = new URLClassLoader(new java.net.URL[]{library.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                assertSame(loader, Class.forName("org.joml.Quaternionfc", false, loader).getClassLoader());
                assertNotNull(Class.forName("org.joml.Vector3f", true, loader).getConstructor().newInstance());
            }
        } finally {
            Files.deleteIfExists(library);
        }
    }
}
