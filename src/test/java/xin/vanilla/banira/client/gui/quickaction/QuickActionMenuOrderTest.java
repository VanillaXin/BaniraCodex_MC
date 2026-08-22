package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 右键菜单自定义顺序必须可持久化，并能容纳后注册的入口。 */
public class QuickActionMenuOrderTest {
    @Test
    public void movesRowsAndKeepsNewRegistryEntries() {
        QuickActionLayout layout = new QuickActionLayout();
        layout.syncMenuItemOrder(Arrays.asList("entry:a", "entry:b", "entry:c"));

        assertTrue(layout.moveMenuItem("entry:c", -1));
        assertEquals(Arrays.asList("entry:a", "entry:c", "entry:b"), layout.menuItemOrder());

        layout.syncMenuItemOrder(Arrays.asList("entry:a", "entry:c", "entry:d"));
        assertEquals(Arrays.asList("entry:a", "entry:c", "entry:d"), layout.menuItemOrder());
    }

    @Test
    public void menuOrderRoundTripsThroughJson() {
        QuickActionLayout source = new QuickActionLayout();
        source.syncMenuItemOrder(Arrays.asList("entry:b", "entry:a"));

        QuickActionLayout restored = new QuickActionLayout();
        restored.fromJson(source.toJson());

        assertEquals(Arrays.asList("entry:b", "entry:a"), restored.menuItemOrder());
    }
}
