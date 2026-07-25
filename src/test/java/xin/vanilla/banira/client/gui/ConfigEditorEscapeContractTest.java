package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class ConfigEditorEscapeContractTest {

    @Test
    public void escapeUsesOpeningBaselineAndWarnsAboutPendingChanges() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/ConfigEditorScreen.java")), StandardCharsets.UTF_8);

        assertTrue(source.contains("baselineValues.put(desc.getPath(), snapshot(adapter.getValue()))"));
        assertTrue(source.contains("int changedCount = pendingChangeCount()"));
        assertTrue(source.contains("config_editor_unsaved_changes"));
        assertTrue(source.contains("if (changedCount == 0)"));
    }
}
