package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/** 管理界面直接使用的动态词条必须随发布语言一起存在。 */
public class CustomQuickActionEditorContractTest {
    private static final List<String> REQUIRED_SUFFIXES = Arrays.asList(
            "custom_quick_action_title", "custom_quick_action_edit_title",
            "custom_quick_action_step_title", "custom_quick_action_step_edit_title",
            "custom_quick_action_display_icon", "custom_quick_action_display_list_only",
            "custom_quick_action_icon_item", "custom_quick_action_icon_effect",
            "custom_quick_action_icon_resource", "custom_quick_action_icon_external_file",
            "custom_quick_action_execution_parallel", "custom_quick_action_execution_chained",
            "custom_quick_action_step_type_command", "custom_quick_action_step_type_screen",
            "custom_quick_action_condition_always", "custom_quick_action_condition_on_success",
            "custom_quick_action_condition_on_failure", "custom_quick_action_close_before",
            "custom_quick_action_edit", "custom_quick_action_add_menu",
            "custom_quick_action_menu_step_title", "custom_quick_action_menu_edit_title"
    );

    @Test
    public void editorTranslationsExistInBundledLanguages() throws Exception {
        assertTranslations("zh_cn");
        assertTranslations("en_us");
    }

    @Test
    public void editorSupportsStrictEnumsCustomScreenTargetsAndIconPickers() throws Exception {
        String editor = source("CustomQuickActionEditor.java");
        String steps = source("CustomQuickActionStepsScreen.java");
        assertTrue(editor.contains("DropdownInputMode.SELECTION_ONLY"));
        assertTrue(editor.contains("DropdownInputMode.EDITABLE"));
        assertTrue(steps.contains("new ItemSelectScreen"));
        assertTrue(steps.contains("new EffectSelectScreen"));
        assertTrue(steps.contains("WidgetType.FILE"));
        assertTrue(steps.contains("popupOption.addOptionWithId"));
        assertTrue(steps.contains("PresetStyle.DELETE"));
        assertTrue(steps.contains("definition.getContextMenuItems()"));
        assertTrue(editor.contains("custom_quick_action_close_before"));
        assertTrue(!editor.contains(".disabled(mode == QuickActionExecutionMode.PARALLEL)"));
    }

    private static void assertTranslations(String language) throws Exception {
        Path path = Paths.get("src/main/resources/assets/banira_codex/lang/" + language + ".json");
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (String suffix : REQUIRED_SUFFIXES) {
                assertTrue(language + " missing " + suffix,
                        json.has("word.banira_codex." + suffix));
            }
        }
    }

    private static String source(String name) throws Exception {
        Path path = Paths.get("src/main/java/xin/vanilla/banira/client/gui/quickaction", name);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
