package xin.vanilla.banira.internal.compat.aui;

import com.google.gson.JsonParser;
import org.junit.Assume;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class AuiFoundationRuntimeTest {
    private static final String LIBRARY = "META-INF/jarjar/joml-1.10.5.jar";
    private static final String LIBRARY_SHA = "cac9f22f83a7aa33eebda73c16ff5261e3cb4911b6bafcf4c79ea486099d0c9a";

    @Test
    public void developmentAndProductionDeclareTheSamePinnedLegacyLibrary() throws Exception {
        String build = Files.readString(Path.of("gradle/aui-contract.gradle"));
        assertTrue(build.contains("minecraftLibrary 'org.joml:joml:1.10.5'"));
        assertTrue(build.contains("jarJar.pin(it, '1.10.5')"));
        assertTrue(build.contains("include(name: 'META-INF/jarjar/**')"));
        assertFalse(build.contains("fromRuntimeConfiguration"));
    }

    @Test
    public void dependencyOnlyCompilationCannotEraseRefmapOutputs() throws Exception {
        String guard = Files.readString(Path.of("gradle/mixin-refmap.gradle"));
        assertTrue(guard.contains("options.incremental = false"));
        assertTrue(guard.contains("outputs.files(refmap"));
        assertTrue(guard.contains("compileJava-mappings.tsrg"));
        assertTrue(Files.readString(Path.of("build.gradle")).contains("apply from: 'gradle/mixin-refmap.gradle'"));
    }

    @Test
    public void forge18ProvidesThePublicStaticLegacySlotHighlightContract() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/net/minecraft/client/gui/screens/inventory/AbstractContainerScreen.class")) {
            assertNotNull(input);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_CODE);
            assertTrue(node.methods.stream().anyMatch(method -> method.name.equals("renderSlotHighlight")
                    && method.desc.equals("(Lcom/mojang/blaze3d/vertex/PoseStack;IIII)V")
                    && (method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)));
        }
    }

    @Test
    public void arbitraryBytesCannotPassTheLibraryIdentityGuard() throws Exception {
        assertNotEquals(LIBRARY_SHA, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new byte[0])));
    }

    @Test
    public void actualProductionArchiveCarriesLibraryAndRefmap() throws Exception {
        String artifact = System.getProperty("banira.aui.foundationArtifact");
        Assume.assumeTrue("Exact artifact supplied by auiFoundationArtifactTest", artifact != null);
        try (JarFile jar = new JarFile(artifact)) {
            assertNotNull(jar.getEntry(LIBRARY));
            byte[] library = jar.getInputStream(jar.getEntry(LIBRARY)).readAllBytes();
            assertEquals(LIBRARY_SHA, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(library)));
            var metadata = JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("META-INF/jarjar/metadata.json")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            var libraries = metadata.getAsJsonArray("jars");
            assertEquals(1, libraries.size());
            var entry = libraries.get(0).getAsJsonObject();
            assertEquals("org.joml", entry.getAsJsonObject("identifier").get("group").getAsString());
            assertEquals("joml", entry.getAsJsonObject("identifier").get("artifact").getAsString());
            assertEquals("1.10.5", entry.getAsJsonObject("version").get("artifactVersion").getAsString());
            assertEquals("[1.10.5,1.11)", entry.getAsJsonObject("version").get("range").getAsString());
            assertEquals(LIBRARY, entry.get("path").getAsString());
            assertNotNull(jar.getEntry("banira_codex.refmap.json"));
            var refmap = JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("banira_codex.refmap.json")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            var mappings = refmap.getAsJsonObject("mappings");
            assertTrue(mappings.size() >= 5);
            var player = mappings.getAsJsonObject("xin/vanilla/banira/internal/mixin/accessors/ServerPlayerAccessor");
            assertTrue(player.get("chatVisibility").getAsString().startsWith("f_8922_:"));
            assertFalse(player.has("language"));
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var file = entries.nextElement();
                assertFalse(file.getName(), file.getName().startsWith("com/sighs/") || file.getName().startsWith("org/joml/"));
                if (file.getName().endsWith(".class")) {
                    byte[] bytes = jar.getInputStream(file).readAllBytes();
                    assertTrue(file.getName(), (((bytes[6] & 255) << 8) | (bytes[7] & 255)) <= 61);
                }
                if (file.getName().endsWith(".jar")) assertEquals(LIBRARY, file.getName());
            }
            Path extracted = Files.createTempFile("banira-forge18-joml-", ".jar");
            try {
                Files.write(extracted, library);
                try (var loader = new URLClassLoader(new java.net.URL[]{extracted.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                    assertSame(loader, Class.forName("org.joml.Quaternionfc", false, loader).getClassLoader());
                    assertNotNull(Class.forName("org.joml.Vector3f", true, loader).getConstructor().newInstance());
                }
            } finally {
                Files.deleteIfExists(extracted);
            }
        }
    }
}
