package xin.vanilla.banira.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** 锁定带修改数量的 ESC 提示使用 format 命名空间。 */
public class ConfigEditorUnsavedTranslationTest {
    @Test
    public void unsavedWarningUsesFormatTranslationInBothLanguages() throws Exception {
        assertTranslation("zh_cn", "当前已有%s项配置被修改，\n取消并关闭界面请点击关闭按钮");
        assertTranslation("en_us", "%s config entries have been changed.\nUse the Close button to discard them and close this screen.");
    }

    private static void assertTranslation(String language, String expected) throws Exception {
        Path path = Paths.get("src/main/resources/assets/banira_codex/lang/" + language + ".json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            assertEquals(expected, json.get("format.banira_codex.config_editor_unsaved_changes").getAsString());
            assertFalse(json.has("word.banira_codex.config_editor_unsaved_changes"));
        }
    }
}
