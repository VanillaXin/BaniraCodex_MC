package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 表单公开密码类型、可编辑下拉模式和非输入焦点回车提交。 */
public class InputFormInputModeContractTest {
    @Test
    public void formExposesNewInputModes() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/InputFormScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("PASSWORD,"));
        assertTrue(source.contains("dd.inputMode(widget.dropdownInputMode())"));
        assertTrue(source.contains("&& !inputFocused && canSubmit()"));
    }
}
