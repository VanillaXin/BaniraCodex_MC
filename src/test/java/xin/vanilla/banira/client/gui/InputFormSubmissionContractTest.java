package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InputFormSubmissionContractTest {
    @Test
    public void validationDisablesSubmitWithoutTurningItIntoCancel() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/InputFormScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("submitButtonWidget.enabled(canSubmit())"));
        assertFalse(source.contains("submitButtonWidget.text(Text.transAuto(BaniraCodex.MODID, \"cancel\"))"));
        assertFalse(source.contains("submitButtonWidget.text().content().equals"));
    }
}
