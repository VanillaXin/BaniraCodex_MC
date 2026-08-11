package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ButtonLongPressParityContractTest {

    @Test
    public void usesTheSharedCheckmarkCompletionFeedback() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/widget/ButtonWidget.java")),
                StandardCharsets.UTF_8);
        String guiUtils = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/util/AbstractGuiUtils.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("COMPLETION_KIND_CHECK"));
        assertTrue(source.contains("spawnLongPressCompletionFeedback"));
        assertTrue(source.contains("drawLongPressCompletionCheck"));
        assertTrue(source.contains("LongPressCompletionEffect"));
        assertTrue(source.contains("drawLineWithSquareCaps"));
        assertTrue(guiUtils.contains("public static void drawLineWithSquareCaps"));
        assertTrue(guiUtils.contains("float capExtension = lineWidth * 0.5f"));
        assertFalse(source.contains("spawnLongPressBurst"));
    }
}
