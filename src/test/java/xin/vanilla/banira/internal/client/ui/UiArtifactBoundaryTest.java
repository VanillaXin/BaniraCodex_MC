package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

public class UiArtifactBoundaryTest {
    static List<String> violations(Map<String, byte[]> files, boolean merged) { return UiBoundaryChecks.artifact(files, "xin/vanilla/banira/", merged); }
    @Test public void normalOwnedFilesAreAllowed() {
        var writer = new org.objectweb.asm.ClassWriter(0); writer.visit(61, 1, "xin/vanilla/banira/Example", null, "java/lang/Object", null); writer.visitEnd();
        assertTrue(violations(Map.of("xin/vanilla/banira/Example.class", writer.toByteArray(),
                "assets/apricityui/apricity/banira_codex/ui/example.html", "<main></main>".getBytes(StandardCharsets.UTF_8)), false).isEmpty());
    }
    @Test public void pomAndNestedArchiveLeaksAreRejected() throws Exception {
        assertTrue(UiBoundaryChecks.pom("<project/>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertFalse(UiBoundaryChecks.pom("<project><dependencies><dependency><artifactId>apricityui</artifactId></dependency></dependencies></project>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        var output = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(output)) { zip.putNextEntry(new java.util.zip.ZipEntry("com/sighs/apricityui/Engine.class")); zip.write(new byte[0]); zip.closeEntry(); }
        assertFalse(violations(Map.of("META-INF/jarjar/engine.jar", output.toByteArray()), true).isEmpty());
    }
    @Test public void formalArchivesContainOnlyProductFiles() throws Exception {
        String inputs = System.getProperty("banira.ui.artifacts"); assertNotNull("uiArtifactTest must supply archive providers", inputs);
        for (String file : inputs.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            assertTrue(file, java.nio.file.Files.isRegularFile(java.nio.file.Path.of(file)));
            assertEquals(file, List.of(), violations(UiBoundaryChecks.readZip(java.nio.file.Path.of(file)), file.endsWith("-all.jar")));
        }
        String poms = System.getProperty("banira.ui.poms", "");
        if (!poms.isEmpty()) for (String pom : poms.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
            assertEquals(pom, List.of(), UiBoundaryChecks.pom(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(pom))));
    }
    @Test public void injectedEngineDeclarationsTestsAndProbesAreRejected() {
        for (String path : List.of("com/sighs/apricityui/init/Document.class", "com/sighs/apricityui/init/Document.java",
                "META-INF/banira-aui-compile-api.properties", "xin/vanilla/banira/InjectedTest.class",
                "xin/vanilla/banira/RuntimeProbe.class", "docs/task.md")) {
            assertFalse("Accepted injected " + path, violations(Map.of(path, new byte[0]), false).isEmpty());
        }
    }
    @Test public void futureClassVersionsAndUnsafePathsAreRejected() {
        for (String path : List.of("../escape.class", "assets/../foreign.css", "C:/foreign.class"))
            assertFalse(path, violations(Map.of(path, new byte[0]), false).isEmpty());
        byte[] header = {(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe, 0, 0, 0, 62};
        assertFalse(violations(Map.of("xin/vanilla/banira/Newer.class", header), false).isEmpty());
    }
    @Test public void opaqueEngineCoordinatesAreRejected() {
        String xml = "<project><dependencies><dependency><groupId>maven.modrinth</groupId><artifactId>UfPdUeGy</artifactId></dependency></dependencies></project>";
        assertFalse(UiBoundaryChecks.pom(xml.getBytes(StandardCharsets.UTF_8)).isEmpty());
    }
    @Test public void namespacedEngineDependenciesAreRejected() {
        String xml = "<m:project xmlns:m='http://maven.apache.org/POM/4.0.0'><m:dependencies><m:dependency><m:artifactId>apricityui</m:artifactId></m:dependency></m:dependencies></m:project>";
        assertFalse(UiBoundaryChecks.pom(xml.getBytes(StandardCharsets.UTF_8)).isEmpty());
    }
    @Test public void localAgentInstructionsAreRejected() {
        assertFalse(violations(Map.of("AGENTS.md", new byte[0]), false).isEmpty());
    }
}
