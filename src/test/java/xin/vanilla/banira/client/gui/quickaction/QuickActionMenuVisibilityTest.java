package xin.vanilla.banira.client.gui.quickaction;

import com.google.gson.JsonObject;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuickActionMenuVisibilityTest {
    @Test
    public void contextMenuItemExposesStableRegistrationId() throws Exception {
        Constructor<QuickActionContextMenuItem> constructor = QuickActionContextMenuItem.class
                .getConstructor(String.class, xin.vanilla.banira.common.data.Component.class,
                        Consumer.class);
        QuickActionContextMenuItem item = constructor.newInstance("settings", null, null);
        Method id = QuickActionContextMenuItem.class.getMethod("id");

        assertEquals("settings", id.invoke(item));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void hiddenMenuRowsRoundTripSeparatelyFromHiddenIcons() throws Exception {
        Method hiddenMenuItemIds = QuickActionLayout.class.getMethod("hiddenMenuItemIds");
        QuickActionLayout source = new QuickActionLayout();
        ((Set<String>) hiddenMenuItemIds.invoke(source)).add("entry:test:menu");
        source.userSlotGrid().add("test:icon");
        source.hiddenIconIds().add("test:icon");

        JsonObject json = source.toJson();
        QuickActionLayout restored = new QuickActionLayout();
        restored.fromJson(json);

        assertTrue(((Set<String>) hiddenMenuItemIds.invoke(restored)).contains("entry:test:menu"));
        assertTrue(restored.hiddenIconIds().contains("test:icon"));
        assertFalse(restored.hiddenIconIds().contains("entry:test:menu"));
    }

    @Test
    public void overlayConnectsRightClickHidingAndHiddenRowRecovery() throws Exception {
        String source = source("QuickActionOverlay.java");
        assertTrue(source.contains("hiddenMenuItemIds()"));
        assertTrue(source.contains("hiddenMenuKey"));
        assertTrue(source.contains("CTX_PAGE_ROW_ACTION"));
        assertTrue(source.contains("openContextMenuRowActions"));
        assertTrue(source.contains("quick_action.hide_menu_entry"));
        assertTrue(source.contains("quick_action.hide_menu_item"));
        assertTrue(source.contains("quick_action.unhide_menu_item"));
        assertFalse(source.contains("return hideContextMenuRow(row);"));
    }

    @Test
    public void longContextRowsUseMiddleEllipsisAndKeepTheFullTooltip() throws Exception {
        String source = source("QuickActionOverlay.java");
        assertTrue(source.contains("ellipsizeMiddle"));
        assertTrue(source.contains("contextTooltipLine = full"));

        String zh = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/assets/banira_codex/lang/zh_cn.json")), StandardCharsets.UTF_8);
        assertTrue(zh.contains("\"format.banira_codex.quick_action.unhide_menu_item\": \"恢复 · %s\""));
    }

    private static String source(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction", file)),
                StandardCharsets.UTF_8);
    }
}
