package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

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
}
