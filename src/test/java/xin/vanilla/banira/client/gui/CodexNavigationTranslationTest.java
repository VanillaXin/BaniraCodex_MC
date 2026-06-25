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

/**
 * 导航页直接渲染这些词条；缺失时按钮会显示原始翻译键。
 */
public class CodexNavigationTranslationTest {
    private static final List<String> REQUIRED_KEYS = Arrays.asList(
            "word.banira_codex.codex_navigation_title",
            "word.banira_codex.codex_navigation_notification_log",
            "word.banira_codex.codex_navigation_client_config",
            "word.banira_codex.codex_navigation_common_config",
            "word.banira_codex.custom_player_config_title"
    );

    @Test
    public void codexNavigationKeysExistInBundledLanguages() throws Exception {
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
