package xin.vanilla.banira.common.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;
import xin.vanilla.banira.client.gui.ItemSelectScreen;
import xin.vanilla.banira.internal.client.ui.NativeUiView;
import xin.vanilla.banira.internal.client.ui.page.SelectorUiSession;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class ItemVariantCatalogTest {
    private CreativeModeTab tab;
    private Field contents;
    private Object original;

    @BeforeClass
    public static void bootstrap() throws Exception {
        xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.bootstrapGame();
    }

    @Before
    public void installVariants() throws Exception {
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(
                xin.vanilla.banira.internal.forge.platform.ForgeBaniraRegistryService.INSTANCE));
        tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.COMBAT);
        assertNotNull(tab);
        contents = CreativeModeTab.class.getDeclaredField("displayItems");
        contents.setAccessible(true);
        original = contents.get(tab);
        ItemStack potion = marked(PotionContents.createItemStack(Items.POTION, Potions.HEALING));
        ItemStack strong = marked(PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING));
        ItemStack arrow = marked(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.POISON));
        ItemStack slow = marked(PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.SLOWNESS));
        ItemStack sharp = marked(book(Enchantments.SHARPNESS, 1));
        ItemStack fortune = marked(book(Enchantments.FORTUNE, 3));
        List<ItemStack> variants = new ArrayList<>(Arrays.asList(potion, strong, arrow, slow, sharp, fortune, potion.copy()));
        for (Item container : Arrays.asList(Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            variants.add(marked(PotionContents.createItemStack(container, Potions.HEALING)));
            variants.add(marked(PotionContents.createItemStack(container, Potions.STRONG_HEALING)));
        }
        contents.set(tab, variants);
        ItemUtils.clearCache();
    }

    @After
    public void restore() throws Exception {
        if (contents != null && tab != null) contents.set(tab, original);
        ItemUtils.clearCache();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private static ItemStack marked(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("banira_variant_fixture", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static ItemStack book(ResourceKey<Enchantment> key, int level) {
        Holder<Enchantment> enchantment = VanillaRegistries.createLookup().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemEnchantments.Mutable contents = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        contents.upgrade(enchantment, level);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, contents.toImmutable());
        return book;
    }

    private List<ItemStack> variants(Item item) {
        return ItemUtils.getAllItems().stream().filter(stack -> stack.is(item)
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("banira_variant_fixture")).toList();
    }

    @Test
    public void retainsDistinctPotionTagsAndDropsOnlyExactDuplicates() {
        for (Item container : Arrays.asList(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            List<ItemStack> variants = variants(container);
            assertEquals(2, variants.size());
            assertFalse(ItemStack.isSameItemSameComponents(variants.get(0), variants.get(1)));
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
        List<ItemStack> refreshed = new ArrayList<>((Collection<ItemStack>) contents.get(tab));
        refreshed.add(marked(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)));
        contents.set(tab, refreshed);
        assertEquals(3, variants(Items.POTION).size());
        assertNotSame(snapshot, cache.get(null));
    }

    @Test
    public void searchCacheDistinguishesPotionComponentsOutsideCustomData() throws Exception {
        var method = ItemUtils.class.getDeclaredMethod("getItemStackCacheKey", ItemStack.class);
        method.setAccessible(true);
        assertNotEquals(method.invoke(null, PotionContents.createItemStack(Items.POTION, Potions.HEALING)),
                method.invoke(null, PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING)));
    }

    private SelectorUiSession selector(ItemStack initial) {
        return new SelectorUiSession(new ItemSelectScreen.Args()
                .parentScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) {
                }).defaultItem(initial).onDataReceived1(value -> {
                }));
    }

    @Test
    public void nestedBundleComponentsHaveDistinctStableSelectionKeys() throws Exception {
        ItemStack first = new ItemStack(Items.BUNDLE);
        first.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(PotionContents.createItemStack(Items.POTION, Potions.HEALING))));
        ItemStack second = new ItemStack(Items.BUNDLE);
        second.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING))));
        assertEquals(first.getComponentsPatch().toString(), second.getComponentsPatch().toString());
        String firstKey = ItemUtils.getItemStackCacheKey(first);
        String secondKey = ItemUtils.getItemStackCacheKey(second);
        assertNotEquals(firstKey, secondKey);
        assertEquals(firstKey, ItemUtils.getItemStackCacheKey(first.copyWithCount(4)));
        contents.set(tab, List.of(first, second));
        var session = selector(ItemStack.EMPTY);
        session.bind(new NativeUiView(new Object()));
        session.select(secondKey);
        assertTrue(ItemStack.isSameItemSameComponents(second, session.item()));
        session.select(firstKey);
        assertTrue(ItemStack.isSameItemSameComponents(first, session.item()));
        ItemUtils.clearCache();
        assertNotEquals(firstKey, ItemUtils.getItemStackCacheKey(first));
    }

    private record OpaqueValue(int value) {
        @Override
        public String toString() {
            return "opaque";
        }
    }

    @Test
    public void cacheKeysUseComponentValuesRatherThanTheirDisplayStrings() {
        var type = net.minecraft.core.component.DataComponentType.<OpaqueValue>builder()
                .persistent(com.mojang.serialization.Codec.INT.xmap(OpaqueValue::new, OpaqueValue::value)).build();
        ItemStack first = new ItemStack(Items.DIAMOND);
        ItemStack second = first.copy();
        first.set(type, new OpaqueValue(1));
        second.set(type, new OpaqueValue(2));
        assertEquals(first.getComponentsPatch().toString(), second.getComponentsPatch().toString());
        assertNotEquals(ItemUtils.getItemStackCacheKey(first), ItemUtils.getItemStackCacheKey(second));
        assertEquals(ItemUtils.getItemStackCacheKey(first), ItemUtils.getItemStackCacheKey(first.copy()));
    }

    @Test
    public void selectingAnAuiPotionRowRetainsItsComponents() {
        ItemStack chosen = variants(Items.POTION).get(1);
        var session = selector(ItemStack.EMPTY);
        session.bind(new NativeUiView(new Object()));
        session.select(ItemUtils.getItemStackCacheKey(chosen));
        assertTrue(ItemStack.isSameItemSameComponents(chosen, session.item()));
    }

    @Test
    public void nativeSelectionKeepsAnIndependentComponentCopy() {
        ItemStack chosen = variants(Items.ENCHANTED_BOOK).get(1);
        var session = selector(ItemStack.EMPTY);
        session.selectItem(chosen);
        chosen.remove(DataComponents.STORED_ENCHANTMENTS);
        assertNotNull(session.item().get(DataComponents.STORED_ENCHANTMENTS));
    }

    @Test
    public void editingCustomDataDoesNotDiscardPotionContentsOrCount() {
        ItemStack chosen = variants(Items.POTION).get(1).copyWithCount(7);
        var session = selector(chosen);
        var editor = session.editor("nbt");
        editor.draft(0, "{test_value:1}");
        assertTrue(editor.submit());
        assertEquals(chosen.get(DataComponents.POTION_CONTENTS), session.item().get(DataComponents.POTION_CONTENTS));
        assertEquals(7, session.item().getCount());
        assertEquals(1, session.item().get(DataComponents.CUSTOM_DATA).copyTag().getInt("test_value"));
    }
}
