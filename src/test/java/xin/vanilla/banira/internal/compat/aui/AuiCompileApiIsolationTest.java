package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class AuiCompileApiIsolationTest {
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
