package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class AuiCompileApiIsolationTest {
    @Test public void officialMavenIsExclusiveForThePinnedAuiModule() throws Exception {
        String script = Files.readString(Path.of("gradle/aui-compile-api.gradle"), StandardCharsets.UTF_8);
        assertTrue(script.contains("https://maven.sighs.cc/repository/maven-public/"));
        assertTrue(script.contains("exclusiveContent"));
        assertTrue(script.contains("forRepositories(fg.repository)"));
        assertTrue(script.contains("includeModule('com.sighs', 'ApricityUI-forge-1.20.1')"));
        assertFalse(script.contains("maven.modrinth:UfPdUeGy"));
    }

    @Test public void compilationAndContractUseTheSameFixedOfficialVersion() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        String script = Files.readString(Path.of("gradle/aui-compile-api.gradle"), StandardCharsets.UTF_8);
        var properties = new java.util.Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        assertEquals("1.2.9", properties.getProperty("aui_version"));
        assertTrue(build.contains("com.sighs:ApricityUI-forge-1.20.1:${aui_version}"));
        assertTrue(script.contains("com.sighs:ApricityUI-forge-1.20.1:${aui_version}"));
        assertFalse(build.contains("maven.modrinth:UfPdUeGy"));
    }

    @Test public void explicitCompileOnlyConfigurationHasPermanentValidationEntry() throws Exception {
        Path script = Path.of("gradle/aui-compile-api.gradle");
        assertTrue("Missing isolated AUI compile configuration", Files.exists(script));
        String text = Files.readString(script, StandardCharsets.UTF_8);
        for (String required : new String[]{"banira.auiCompileApi", "auiCompileApi", "verifyAuiCompileApi", "runtimeClasspath", "testRuntimeClasspath"}) assertTrue(required, text.contains(required));
    }
    @Test public void compileFixturesAreNotInTheBusinessTestRuntime() throws Exception {
        assertNull(getClass().getClassLoader().getResource("META-INF/banira-aui-compile-api.properties"));
    }
    @Test public void committedContractMatchesPinnedOfficialSignatures() throws Exception {
        var expected = AuiCompileApiTool.contract(AuiCompileApiTool.declarations(Path.of(System.getProperty("banira.aui.contractJar"))));
        var actual = Files.readString(Path.of("gradle/aui-compile-contract.json"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(new String(expected, StandardCharsets.UTF_8), actual);
    }
}
