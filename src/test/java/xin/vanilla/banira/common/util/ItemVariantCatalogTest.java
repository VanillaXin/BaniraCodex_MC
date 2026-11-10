package xin.vanilla.banira.common.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ItemVariantCatalogTest {
    private CreativeModeTab tab;
    private CreativeModeTab original;
    private List<ItemStack> contents;

    @BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void installVariants() throws Exception {
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(
                xin.vanilla.banira.internal.fabric.platform.FabricBaniraRegistryService.INSTANCE));
        original = CreativeModeTab.TABS[CreativeModeTab.TAB_COMBAT.getId()];
        ItemStack potion = marked(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING));
        ItemStack strong = marked(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.STRONG_HEALING));
        ItemStack arrow = marked(PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.POISON));
        ItemStack slow = marked(PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.SLOWNESS));
        ItemStack sharp = marked(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1)));
        ItemStack fortune = marked(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.BLOCK_FORTUNE, 3)));
        List<ItemStack> variants = new ArrayList<>(Arrays.asList(potion, strong, arrow, slow, sharp, fortune, potion.copy()));
        for (Item container : Arrays.asList(Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            variants.add(marked(PotionUtils.setPotion(new ItemStack(container), Potions.HEALING)));
            variants.add(marked(PotionUtils.setPotion(new ItemStack(container), Potions.STRONG_HEALING)));
        }
        contents = variants;
        installTab();
        ItemUtils.clearCache();
    }

    @After
    public void restore() throws Exception {
        if (original != null) CreativeModeTab.TABS[original.getId()] = original;
        ItemUtils.clearCache();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private static ItemStack marked(ItemStack stack) {
        stack.getOrCreateTag().putBoolean("banira_variant_fixture", true);
        return stack;
    }

    private void installTab() {
        tab = new CreativeModeTab(original.getId(), "banira_variant_fixture") {
            public ItemStack makeIcon() {
                return new ItemStack(Items.APPLE);
            }

            public void fillItemList(NonNullList<ItemStack> items) {
                items.addAll(contents);
            }
        };
    }

    private List<ItemStack> variants(Item item) {
        return ItemUtils.getAllItems().stream().filter(stack -> stack.getItem() == item && stack.hasTag()
                && stack.getTag().getBoolean("banira_variant_fixture")).collect(java.util.stream.Collectors.toList());
    }

    @Test
    public void retainsDistinctPotionTagsAndDropsOnlyExactDuplicates() {
        for (Item container : Arrays.asList(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            List<ItemStack> variants = variants(container);
            assertEquals(2, variants.size());
            assertNotEquals(ItemUtils.serializeItemStack(variants.get(0)), ItemUtils.serializeItemStack(variants.get(1)));
        }
    }

    @Test
    public void retainsDistinctTippedArrowEffects() {
        assertEquals(2, variants(Items.TIPPED_ARROW).size());
    }

    @Test
    public void retainsDistinctEnchantedBooks() {
        assertEquals(2, variants(Items.ENCHANTED_BOOK).size());
    }

    @Test
    public void refreshesAfterCreativeContentsChangeWithoutRebuildingEveryQuery() throws Exception {
        List<ItemStack> first = variants(Items.POTION);
        assertEquals(2, first.size());
        Field cache = ItemUtils.class.getDeclaredField("allItemsCache");
        cache.setAccessible(true);
        Object snapshot = cache.get(null);
        variants(Items.POTION);
        assertSame(snapshot, cache.get(null));
        List<ItemStack> refreshed = new ArrayList<>(contents);
        refreshed.add(marked(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS)));
        contents = refreshed;
        installTab();
        assertEquals(3, variants(Items.POTION).size());
        assertNotSame(snapshot, cache.get(null));
    }
}
