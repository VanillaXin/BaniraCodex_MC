package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class AuiCompileApiIsolationTest {
    @Test
    public void officialMavenIsExclusiveForThePinnedAuiModule() throws Exception {
        String script = Files.readString(Path.of("gradle/aui-compile-api.gradle"), StandardCharsets.UTF_8);
        assertTrue(script.contains("https://maven.sighs.cc/repository/maven-public/"));
        assertTrue(script.contains("exclusiveContent"));
        assertTrue(script.contains("forRepositories(fg.repository)"));
        assertTrue(script.contains("includeModule('com.sighs', 'ApricityUI-forge-1.20.1')"));
        assertFalse(script.contains("maven.modrinth:UfPdUeGy"));
    }

    @Test
    public void compilationAndContractUseTheSameFixedOfficialVersion() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        String script = Files.readString(Path.of("gradle/aui-compile-api.gradle"), StandardCharsets.UTF_8);
        var properties = new java.util.Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        assertEquals("1.3.0", properties.getProperty("aui_version"));
        assertTrue(build.contains("com.sighs:ApricityUI-forge-1.20.1:${aui_version}"));
        assertTrue(script.contains("com.sighs:ApricityUI-forge-1.20.1:${aui_version}"));
        assertFalse(build.contains("maven.modrinth:UfPdUeGy"));
    }

    @Test
    public void explicitCompileOnlyConfigurationHasPermanentValidationEntry() throws Exception {
        Path script = Path.of("gradle/aui-compile-api.gradle");
        assertTrue("Missing isolated AUI compile configuration", Files.exists(script));
        String text = Files.readString(script, StandardCharsets.UTF_8);
        for (String required : new String[]{"banira.auiCompileApi", "auiCompileApi", "verifyAuiCompileApi", "runtimeClasspath", "testRuntimeClasspath"})
            assertTrue(required, text.contains(required));
    }

    @Test
    public void compileFixturesAreNotInTheBusinessTestRuntime() throws Exception {
        assertNull(getClass().getClassLoader().getResource("META-INF/banira-aui-compile-api.properties"));
    }

    @Test
    public void committedContractMatchesPinnedOfficialSignatures() throws Exception {
        var expected = AuiCompileApiTool.contract(AuiCompileApiTool.declarations(Path.of(System.getProperty("banira.aui.contractJar"))));
        var actual = Files.readString(Path.of("gradle/aui-compile-contract.json"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertEquals(new String(expected, StandardCharsets.UTF_8), actual);
    }

    @Test
    public void officialVersionAndHashRemainPinnedTogether() throws Exception {
        assertEquals("2C2A8B47B87E6397EEC823DBE978CA8AB1E96C1AD57EA61C1CC83E5FCABFA210", AuiCompileApiTool.SOURCE);
        String script = Files.readString(Path.of("gradle/aui-compile-api.gradle"), StandardCharsets.UTF_8);
        assertTrue(script.contains("def sourceSha = '" + AuiCompileApiTool.SOURCE + "'"));
        assertFalse(script.contains("080AC4987E2EEBEF174287F106AD53132D1B874728F0BBD6A8AB1F74F7188EBF"));
    }

    @Test
    public void unpinnedInputCannotGenerateCompileDeclarations() throws Exception {
        Path input = Files.createTempFile("aui-unpinned-", ".jar");
        try {
            Files.write(input, new byte[]{1, 3, 0});
            try {
                AuiCompileApiTool.declarations(input);
                fail("An unpinned archive must not generate declarations");
            } catch (IllegalArgumentException expected) {
                assertEquals("Unpinned official AUI input", expected.getMessage());
            }
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void selectedGenericReturnTypesRemainInDeclarations() throws Exception {
        var declarations = AuiCompileApiTool.declarations(Path.of(System.getProperty("banira.aui.contractJar")));
        assertTrue(declarations.containsKey(AuiCompileApiTool.ROOT + "loader/Loader$StaticResourceEntry"));
    }

    @Test
    public void generatedDeclarationsCompileEverySelectedMember() throws Exception {
        Path output = Files.createTempDirectory("aui-declaration-contract-");
        try {
            AuiCompileApiTool.generate(Path.of(System.getProperty("banira.aui.contractJar")), output);
            var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
            assertNotNull("A JDK compiler is required to validate the declarations", compiler);
            Path classes = Files.createDirectories(output.resolve("classes"));
            var errors = new java.io.ByteArrayOutputStream();
            int code = compiler.run(null, null, errors, "--release", "17", "-encoding", "UTF-8",
                    "-classpath", output.resolve("api-61.jar").toString(), "-d", classes.toString(),
                    output.resolve("fixture/net/minecraft/client/gui/screens/Screen.java").toString(),
                    output.resolve("fixture/com/mojang/blaze3d/vertex/PoseStack.java").toString(),
                    output.resolve("fixture/AuiDeclarationFixture.java").toString());
            assertEquals(errors.toString(StandardCharsets.UTF_8), 0, code);
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.deleteIfExists(path);
            }
        }
    }
}
