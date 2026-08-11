package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 锁定弹出菜单的鼠标键语义，避免右键确认被静默降级为左键。
 */
public class PopupOptionMouseButtonContractTest {
    private static final Path POPUP = Paths.get(
            "src/main/java/xin/vanilla/banira/client/gui/widget/PopupOption.java");

    @Test
    public void selectionPreservesThePressedMouseButton() {
        String source = read(POPUP);

        assertTrue(source.contains("private int pressedMouseButton = -1;"));
        assertTrue(source.contains("pressedMouseButton = event.button();"));
        assertTrue(source.contains("event.button() != pressedMouseButton"));
        assertTrue(source.contains("new SelectEvent(idx, optId, text, event.button())"));
        assertFalse(source.contains("event.button() != 0 || !built"));
    }

    private static String read(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Unable to read " + path, exception);
        }
    }
}
