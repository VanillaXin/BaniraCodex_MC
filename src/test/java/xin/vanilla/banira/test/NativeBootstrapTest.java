package xin.vanilla.banira.test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.Test;

import static org.junit.Assert.*;

public class NativeBootstrapTest {
    @Test
    public void offlineBindingRetainsVanillaItemDefaultsAndIsIdempotent() {
        NativeBootstrap.initialize();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        var defaults = sword.getComponents();
        assertEquals(1, sword.getMaxStackSize());
        assertEquals(1561, sword.getMaxDamage());
        assertNotNull(sword.get(DataComponents.ITEM_NAME));
        assertEquals(64, new ItemStack(Items.STONE).getMaxStackSize());
        NativeBootstrap.initialize();
        assertEquals(defaults, new ItemStack(Items.DIAMOND_SWORD).getComponents());
    }
}
