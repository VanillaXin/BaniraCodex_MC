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

    @Test
    public void singleClickSelectsAndConfirmationCanBeConsumedOnlyOnce() {
        assertNull(model.confirm());
        assertFalse(model.click(options, 10));
        assertSame(options, model.selection());
        assertSame(options, model.confirm());
        assertNull(model.confirm());
        assertFalse(model.click(options, 20));
        assertNull(model.selection());
    }

    @Test
    public void doubleClickRequiresTwoQuickClicksOnTheSameItem() {
        QuickActionScreenRows.Item controls = item("One [one]", "Controls", "one.gui.Controls", "");
        assertFalse(model.click(options, 10));
        assertFalse(model.click(controls, 80));
        assertFalse(model.click(controls, 500));
        assertTrue(model.click(controls, 700));
        assertSame(controls, model.confirm());
    }

    @Test
    public void unavailableRowsCannotReplaceTheSelection() {
        assertFalse(model.click(menu, 10));
        assertNull(model.selection());
        assertFalse(model.click(options, 50));
        assertFalse(model.click(menu, 75));
        assertSame(options, model.selection());
    }

    @Test
    public void confirmationCannotTargetAnItemHiddenBySearchOrCollapse() {
        model.rows(items, "Options", false);
        model.click(options, 10);
        model.rows(items, "Menu", false);
        assertNull(model.confirm());
        model.rows(items, "Options", false);
        model.click(options, 20);
        model.toggle(options.group);
        model.rows(items, "Options", false);
        assertNull(model.confirm());
    }

    @Test
    public void changingFilterKeepsAVisibleChoiceButBreaksTheDoubleClickSequence() {
        model.rows(items, "Options", false);
        assertFalse(model.click(options, 10));
        model.resetClick();
        model.rows(items, "Options", true);
        assertSame(options, model.selection());
        assertFalse(model.click(options, 20));
        assertTrue(model.click(options, 30));
    }

    private QuickActionScreenRows.Item item(String group, String title, String details, String reason) {
        return new QuickActionScreenRows.Item(details, group, title, details, reason, () -> {
        });
    }
}
