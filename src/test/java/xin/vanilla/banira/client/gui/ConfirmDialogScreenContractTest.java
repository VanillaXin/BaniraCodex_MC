package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 锁定确认页的可见按钮语义，避免重新引入组合键或鼠标键确认。
 */
public class ConfirmDialogScreenContractTest {

    @Test
    public void destructiveActionUsesVisibleButtonsAndRunsOnce() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/ConfirmDialogScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("confirm.id(\"confirm\")"));
        assertTrue(source.contains("cancel.id(\"cancel\")"));
        assertTrue(source.contains("if (resolved)"));
        assertTrue(source.contains("args.onConfirm().run()"));
        assertFalse(source.contains("GLFW_MOUSE_BUTTON_RIGHT"));
        assertFalse(source.contains("onlyCtrlPressed"));
    }
}
