package xin.vanilla.banira.internal.compat.aui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assume;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

public class AuiFoundationRuntimeTest {
    @Test
    public void compilationDeclarationsRemainOutsideRuntimeAndPublication() throws Exception {
        String contract = text("gradle/aui-contract.gradle");
        assertTrue(contract.contains("configurations.compileOnly.extendsFrom(configurations.auiContract)"));
        assertFalse(contract.contains("runtimeOnly"));
        assertFalse(contract.contains("implementation.extendsFrom"));
        assertFalse(contract.contains("fg.deobf"));
    }

    @Test
    public void dependencyOnlyCompileCannotEraseRefmap() throws Exception {
        String guard = text("gradle/mixin-refmap.gradle");
        assertTrue(guard.contains("options.incremental = false"));
        assertTrue(guard.contains("outputs.files(refmap"));
        assertTrue(text("build.gradle").contains("apply from: 'gradle/mixin-refmap.gradle'"));
    }

    @Test
    public void configurationOnlyTasksDoNotRewriteClientOptions() throws Exception {
        String build = text("build.gradle");
        assertFalse(build.contains("\nprepareBaniraDevFpsOverlay()"));
        assertTrue(build.contains("dependsOn tasks.named('prepareDevFpsOverlay')"));
    }

    @Test
    public void actualProductionArchiveIsJava8AndVendorFree() throws Exception {
        String artifact = System.getProperty("banira.aui.foundationArtifact");
        Assume.assumeTrue("Exact production archive supplied by artifact task", artifact != null);
        try (JarFile jar = new JarFile(artifact)) {
            assertNotNull(jar.getEntry("xin/vanilla/banira/internal/compat/aui/AuiCapabilityContract.class"));
            assertNull(jar.getEntry("META-INF/banira-aui-compile-api.properties"));
            JarEntry refmapEntry = jar.getJarEntry("banira_codex.refmap.json");
            assertNotNull(refmapEntry);
            JsonObject mappings;
            try (InputStream input = jar.getInputStream(refmapEntry)) {
                mappings = new JsonParser().parse(new String(AuiClientIsolationTest.read(input), StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonObject("mappings");
            }
            assertTrue(mappings.size() >= 5);
            JsonObject player = mappings.getAsJsonObject("xin/vanilla/banira/internal/mixin/accessors/ServerPlayerOptionsAccessor");
            assertNotNull(player);
            assertEquals("field_71143_cn:Lnet/minecraft/entity/player/ChatVisibility;", player.get("chatVisibility").getAsString());
            assertEquals("field_71140_co:Z", player.get("canChatColor").getAsString());
            assertFalse(mappings.has("xin/vanilla/banira/internal/mixin/accessors/ServerPlayerAccessor"));
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                assertFalse(entry.getName(), entry.getName().startsWith("com/sighs/") || entry.getName().endsWith(".jar"));
                if (entry.getName().endsWith(".class")) {
                    try (InputStream input = jar.getInputStream(entry)) {
                        byte[] bytes = AuiClientIsolationTest.read(input);
                        assertTrue(entry.getName(), (((bytes[6] & 255) << 8) | (bytes[7] & 255)) <= 52);
                    }
                }
            }
        }
    }

    private String text(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
