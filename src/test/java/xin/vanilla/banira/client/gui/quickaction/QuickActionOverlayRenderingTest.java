package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QuickActionOverlayRenderingTest {
    @Test
    public void contextMenuHeightStaysWithinTwoThirdsOfTheScreen() {
        assertEquals(144, contextMenuMaxBodyHeight(240));
        assertEquals(192, contextMenuMaxBodyHeight(480));
    }

    @Test
    public void contextMenuClipsScrollableRows() throws Exception {
        String source = source("QuickActionOverlay.java");
        assertTrue(source.contains("contextMenuMaxBodyHeight(lastScreenH)"));
        assertTrue(source.contains("pushScissor"));
        assertTrue(source.contains("popScissor"));
    }

    @Test
    public void everyQuickIconRestoresItsRenderState() throws Exception {
        String source = source("QuickIcon.java");
        assertTrue(source.contains("private static void prepareDrawState()"));
        assertTrue(count(source, "prepareDrawState();") >= 2);
    }

    private static int count(String source, String value) {
        int result = 0;
        int index = 0;
        while ((index = source.indexOf(value, index)) >= 0) {
            result++;
            index += value.length();
        }
        return result;
    }

    private static int contextMenuMaxBodyHeight(int screenHeight) {
        try {
            java.lang.reflect.Method method = QuickActionOverlay.class.getDeclaredMethod(
                    "contextMenuMaxBodyHeight", int.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, screenHeight);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String source(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction", file)),
                StandardCharsets.UTF_8);
    }
}
