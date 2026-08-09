package xin.vanilla.banira.client.gui.widget;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 密码输入框不得把原文交给渲染器或系统剪贴板。 */
public class InputWidgetSecurityContractTest {
    @Test
    public void passwordUsesMaskedDisplayAndSelection() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/widget/InputWidget.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("String value = displayValue(rawValue)"));
        assertTrue(source.contains("return password ? mask(selected.length()) : selected"));
        assertTrue(source.contains("AbstractGuiUtils.setClipboard(getHighlighted())"));
    }
}
