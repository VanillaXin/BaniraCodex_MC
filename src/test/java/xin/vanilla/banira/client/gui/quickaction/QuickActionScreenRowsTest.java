package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class QuickActionScreenRowsTest {
    private final QuickActionScreenRows model = new QuickActionScreenRows();
    private final QuickActionScreenRows.Item options = item("One [one]", "Options", "one.gui.OptionsScreen", "");
    private final QuickActionScreenRows.Item menu = item("One [one]", "Menu", "one.gui.ContextMenu", "Context required");
    private final QuickActionScreenRows.Item inventory = item("Two [two]", "Inventory", "two.gui.InventoryScreen", "Context required");
    private final List<QuickActionScreenRows.Item> items = Arrays.asList(options, menu, inventory);

    @Test
    public void groupsStartCollapsedAndToggleIndependently() {
        assertEquals(2, model.rows(items, "", false).size());
        model.toggle(options.group);
        List<QuickActionScreenRows.Row> rows = model.rows(items, "", false);
        assertTrue(rows.get(0).expanded);
        assertSame(options, rows.get(1).item);
        assertSame(menu, rows.get(2).item);
        assertFalse(rows.get(3).expanded);
        model.toggle(options.group);
        assertEquals(2, model.rows(items, "", false).size());
    }

    @Test
    public void hideUnavailableRemovesEmptyGroupsAndPreservesExpansion() {
        model.toggle(options.group);
        List<QuickActionScreenRows.Row> rows = model.rows(items, "", true);
        assertEquals(2, rows.size());
        assertSame(options, rows.get(1).item);
        assertEquals(4, model.rows(items, "", false).size());
        assertTrue(model.rows(items, "Inventory", true).isEmpty());
    }

    @Test
    public void searchMatchesFullIdsAndModNamesDespiteShortLabels() {
        assertSame(options, model.rows(items, " ONE.GUI.OPTIONS ", false).get(1).item);
        assertEquals(3, model.rows(items, "[one]", false).size());
    }

    @Test
    public void searchTemporarilyExpandsMatchesWithoutReplacingUserState() {
        model.toggle(inventory.group);
        assertEquals(3, model.rows(items, "one", false).size());
        model.toggle(options.group);
        assertEquals(1, model.rows(items, "one", false).size());
        assertEquals(2, model.rows(items, "Options", false).size());
        List<QuickActionScreenRows.Row> rows = model.rows(items, "", false);
        assertFalse(rows.get(0).expanded);
        assertTrue(rows.get(1).expanded);
        assertSame(inventory, rows.get(2).item);
    }

    @Test
    public void titlesStayShortAndDetailsUseSeparateLines() {
        assertEquals("Menu", menu.title);
        assertFalse(menu.selectable());
        assertEquals("Menu\nOne [one]\none.gui.ContextMenu\nContext required", menu.tooltip());
        assertTrue(options.selectable());
        assertEquals("Options\nOne [one]\none.gui.OptionsScreen", options.tooltip());
    }

    private QuickActionScreenRows.Item item(String group, String title, String details, String reason) {
        return new QuickActionScreenRows.Item(details, group, title, details, reason, () -> {
        });
    }
}
