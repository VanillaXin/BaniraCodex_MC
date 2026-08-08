package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickActionDisplayModeTest {
    @Test
    public void inventoryOnlyDoesNotLeakIntoDefaultMenu() {
        assertTrue(EnumQuickActionDisplay.INVENTORY_ONLY.showsInventoryIcon());
        assertFalse(EnumQuickActionDisplay.INVENTORY_ONLY.showsInDefaultMenu());
        assertTrue(EnumQuickActionDisplay.ICON.showsInventoryIcon());
        assertTrue(EnumQuickActionDisplay.ICON.showsInDefaultMenu());
        assertFalse(EnumQuickActionDisplay.LIST_ONLY.showsInventoryIcon());
        assertTrue(EnumQuickActionDisplay.LIST_ONLY.showsInDefaultMenu());
    }

    @Test
    public void registryExposesInventoryOnlyRegistration() throws Exception {
        QuickActionRegistry.class.getMethod("registerInventoryOnly", String.class,
                QuickIcon.class, xin.vanilla.banira.common.data.Component.class,
                java.util.function.Consumer.class, QuickActionContextMenuItem[].class);
    }

    @Test
    public void inventoryOverlayUsesDisplayCapabilitiesInsteadOfIconConstant() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction/QuickActionOverlay.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("e.display().showsInventoryIcon()"));
        assertFalse(source.contains(".display() != EnumQuickActionDisplay.ICON"));
        assertFalse(source.contains(".display() == EnumQuickActionDisplay.ICON"));
    }

    @Test
    public void displayModeLabelsStateTheirExactSurfaces() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/assets/banira_codex/lang/zh_cn.json")), StandardCharsets.UTF_8);
        JsonObject language = new JsonParser().parse(source).getAsJsonObject();

        assertTrue("同时显示为背包按钮和 Banira 菜单项".equals(language.get(
                "word.banira_codex.custom_quick_action_display_icon").getAsString()));
        assertTrue("仅显示为背包界面按钮".equals(language.get(
                "word.banira_codex.custom_quick_action_display_inventory_only").getAsString()));
        assertTrue("仅显示为 Banira 菜单项".equals(language.get(
                "word.banira_codex.custom_quick_action_display_list_only").getAsString()));
    }
}
