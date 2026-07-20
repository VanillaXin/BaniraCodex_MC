package xin.vanilla.banira.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/** 配置编辑器的按钮、提示和网络结果均不得回退为原始翻译键。 */
public class ConfigEditorTranslationTest {
    private static final List<String> REQUIRED_KEYS = Arrays.asList(
            "word.banira_codex.config_editor_close",
            "word.banira_codex.config_editor_reset_tooltip",
            "word.banira_codex.config_editor_save",
            "word.banira_codex.config_editor_save_success",
            "word.banira_codex.config_editor_save_tooltip",
            "word.banira_codex.config_editor_save_tooltip_network",
            "word.banira_codex.config_editor_sync",
            "word.banira_codex.config_editor_sync_tooltip",
            "word.banira_codex.config_editor_fetch_not_connected",
            "word.banira_codex.config_editor_sync_not_connected",
            "word.banira_codex.config_editor_sync_nothing",
            "word.banira_codex.config_editor_sync_server_empty",
            "word.banira_codex.config_editor_sync_server_no_permission",
            "word.banira_codex.config_editor_sync_server_not_applicable",
            "word.banira_codex.config_editor_title",
            "word.banira_codex.config_editor_validation_failed",
            "format.banira_codex.config_editor_save_failed",
            "format.banira_codex.config_editor_fetch_applied",
            "format.banira_codex.config_editor_fetch_apply_failed",
            "format.banira_codex.config_editor_fetch_send_failed",
            "format.banira_codex.config_editor_sync_failed",
            "format.banira_codex.config_editor_sync_full_failed",
            "format.banira_codex.config_editor_sync_server_ok",
            "format.banira_codex.config_editor_sync_server_save_failed",
            "format.banira_codex.config_editor_sync_server_unknown_config"
    );

    @Test
    public void configEditorKeysExistInBundledLanguages() throws Exception {
        assertLanguageHasKeys("zh_cn");
        assertLanguageHasKeys("en_us");
    }

    private static void assertLanguageHasKeys(String language) throws IOException {
        JsonObject lang = readLanguage(language);
        for (String key : REQUIRED_KEYS) {
            assertTrue(language + " missing " + key, lang.has(key));
        }
    }

    private static JsonObject readLanguage(String language) throws IOException {
        Path path = Paths.get("src", "main", "resources", "assets", "banira_codex", "lang", language + ".json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }
}
