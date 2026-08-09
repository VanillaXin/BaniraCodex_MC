package xin.vanilla.banira.common.config;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class KeyChordConfigContractTest {
    @Test
    public void stringListsCanRequestKeyCaptureInsteadOfFreeText() throws Exception {
        String annotation = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/common/config/annotation/ConfigEntry.java")),
                StandardCharsets.UTF_8);
        String adapter = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/forge/config/ForgeConfigAdapter.java")),
                StandardCharsets.UTF_8);
        String rows = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/ConfigEditorScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(annotation.contains("@interface KeyChords"));
        assertTrue(adapter.contains("ConfigEntry.Gui.KeyChords.class"));
        assertTrue(rows.contains("return TagListEditorWidget.ItemType.KEY_CHORD"));
    }
}
