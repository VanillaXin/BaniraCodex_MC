package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * 锁定四类配置入口的同一搜索契约，防止后续版本迁移漏掉某个页面。
 */
public class ConfigSearchScreenContractTest {

    @Test
    public void allConfigScreensUseSharedSearchAndVisibleTreeReflow() throws Exception {
        String editor = source("ConfigEditorScreen.java");
        String player = source("CustomPlayerConfigEditScreen.java");
        String notification = source("NotificationTypeConfigScreen.java");
        String panel = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/widget/CollapsiblePanelWidget.java")),
                StandardCharsets.UTF_8);
        String label = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/widget/LabelWidget.java")),
                StandardCharsets.UTF_8);

        assertTrue(editor.contains("config_search"));
        assertTrue(editor.contains("ConfigEntryTooltipTexts.guiTooltipComponent"));
        assertTrue(editor.contains("applyNodeFilter(root, query, false)"));
        assertTrue(player.contains("custom_player_config_search"));
        assertTrue(player.contains("custom_player_config_language_description"));
        assertTrue(notification.contains("notification_type_search"));
        assertTrue(notification.contains("NotificationTypeRegistry.tooltip(typeId)"));
        assertTrue(notification.contains("matchesContainingGroup(typeId, query)"));
        assertTrue(editor.contains("theme.searchMatchText()"));
        assertTrue(player.contains("theme.searchMatchText()"));
        assertTrue(notification.contains("theme.searchMatchText()"));
        assertTrue(panel.contains("reflowVisibleChildren()"));
        assertTrue(label.contains("preserveStyledComponent"));
    }

    @Test
    public void bothLanguagesProvideSearchAndPlayerDescriptions() throws Exception {
        for (String language : new String[]{"zh_cn", "en_us"}) {
            String json = new String(Files.readAllBytes(Paths.get(
                    "src/main/resources/assets/banira_codex/lang/" + language + ".json")),
                    StandardCharsets.UTF_8);
            assertTrue(json.contains("\"word.banira_codex.config_search_hint\""));
            assertTrue(json.contains("\"word.banira_codex.custom_player_config_language_description\""));
            assertTrue(json.contains("\"word.banira_codex.custom_player_config_notification_mode_description\""));
        }
    }

    private static String source(String fileName) throws Exception {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/" + fileName)), StandardCharsets.UTF_8);
    }
}
