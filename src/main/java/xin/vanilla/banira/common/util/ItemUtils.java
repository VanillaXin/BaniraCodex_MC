package xin.vanilla.banira.common.util;

import com.mojang.brigadier.StringReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ItemUtils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation UNKNOWN_ITEM = BaniraCodex.resourceFactory().create("unknown_item");

    /**
     * 所有物品缓存
     */
    private static volatile List<ItemStack> allItemsCache = Collections.emptyList();

    /**
     * 物品堆信息缓存
     */
    private static final Map<String, ItemStackInfo> itemStackInfoCache = new ConcurrentHashMap<>();

    /**
     * 物品堆信息类
     */
    private static class ItemStackInfo {
        final String registry;
        final String hoverName;
        final String description;
        final Set<String> tags;

        ItemStackInfo(String registry, String hoverName, String description, Set<String> tags) {
            this.registry = registry;
            this.hoverName = hoverName;
            this.description = description;
            this.tags = tags;
        }
    }

    private ItemUtils() {
    }

    // region 获取物品信息

    /**
     * 获取物品的注册ID
     *
     * @param item 物品
     */
    @Nullable
    public static ResourceLocation getItemRegistry(Item item) {
        if (item == null) return null;
        return item.getRegistryName();
    }

    /**
     * 获取物品的注册ID字符串
     *
     * @param item 物品
     */
    public static String getItemRegistryString(Item item) {
        ResourceLocation registryName = getItemRegistry(item);
        return registryName != null ? registryName.toString() : UNKNOWN_ITEM.toString();
    }

    /**
     * 获取物品堆叠的注册ID
     *
     * @param itemStack 物品堆叠
     */
    @Nullable
    public static ResourceLocation getItemRegistry(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return null;
        return getItemRegistry(itemStack.getItem());
    }

    /**
     * 获取物品堆叠的注册ID字符串
     *
     * @param itemStack 物品堆叠
     */
    public static String getItemRegistryString(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return UNKNOWN_ITEM.toString();
        return getItemRegistryString(itemStack.getItem());
    }

    /**
     * 获取物品名称翻译键
     *
     * @param item 物品
     */
    public static String getItemNameKey(Item item) {
        if (item == null) return "";
        return item.getDescriptionId();
    }

    /**
     * 获取物品名称翻译键
     *
     * @param itemStack 物品
     */
    public static String getItemNameKey(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return "";
        return itemStack.getDescriptionId();
    }

    /**
     * 获取物品显示名称字符串
     *
     * @param itemStack 物品
     */
    public static String getItemHoverNameString(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return "";
        return itemStack.getHoverName().getString();
    }

    /**
     * 获取本地化的物品显示名称字符串
     * <p>
     * 客户端专用
     *
     * @param itemStack 物品
     */
    @OnlyIn(Dist.CLIENT)
    public static String getItemHoverNameStringLocalized(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return "";
        try {
            return itemStack.getHoverName().getString();
        } catch (Exception e) {
            LOGGER.warn("Failed to get localized display name for item stack", e);
            return getItemHoverNameString(itemStack);
        }
    }

    // endregion

    // region 物品比较

    /**
     * 比较两个物品是否相同
     * <p>
     * 不比较NBT
     *
     * @param stack1 物品1
     * @param stack2 物品2
     */
    public static boolean areItemsEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1 == null || stack2 == null) return false;
        if (stack1.isEmpty() && stack2.isEmpty()) return true;
        if (stack1.isEmpty() || stack2.isEmpty()) return false;
        return ItemStack.isSame(stack1, stack2);
    }

    /**
     * 比较两个物品堆叠是否相同
     * <p>
     * 比较NBT
     *
     * @param stack1 物品1
     * @param stack2 物品2
     */
    public static boolean areItemsEqualWithNBT(ItemStack stack1, ItemStack stack2) {
        if (!areItemsEqual(stack1, stack2)) return false;
        // if (stack1 == null || stack2 == null) return false;
        if (stack1.isEmpty() && stack2.isEmpty()) return true;
        return ItemStack.tagMatches(stack1, stack2);
    }

    /**
     * 比较两个物品是否完全相同
     * <p>
     * 包括数量
     *
     * @param stack1 物品1
     * @param stack2 物品2
     */
    public static boolean areItemsIdentical(ItemStack stack1, ItemStack stack2) {
        if (!areItemsEqualWithNBT(stack1, stack2)) return false;
        if (stack1 == null || stack2 == null) return false;
        return stack1.getCount() == stack2.getCount();
    }

    // endregion

    // region 物品序列化与反序列化

    /**
     * 将物品序列化为字符串
     */
    public static String serializeItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return "";
        try {
            ResourceLocation itemId = getItemRegistry(itemStack);
            if (itemId == null) return "";
            StringBuilder result = new StringBuilder();
            result.append(itemId);
            // 保存物品到NBT
            CompoundNBT nbt = new CompoundNBT();
            itemStack.save(nbt);
            // 移除id字段
            nbt.remove("id");
            if (!nbt.isEmpty()) {
                result.append(nbt);
            }
            return result.toString();
        } catch (Exception e) {
            LOGGER.error("Failed to serialize item stack", e);
            return "";
        }
    }

    /**
     * 从字符串反序列化物品
     */
    public static ItemStack deserializeItemStack(String itemString) {
        if (StringUtils.isNullOrEmptyEx(itemString)) {
            return ItemStack.EMPTY;
        }
        itemString = itemString.trim();
        try {
            ItemParser parse = new ItemParser(new StringReader(itemString), false).parse();
            ItemInput itemInput = new ItemInput(parse.getItem(), parse.getNbt());
            return itemInput.createItemStack(1, false);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize item stack from string: {}", itemString, e);
            return ItemStack.EMPTY;
        }
    }

    // endregion

    // region 获取所有物品列表

    /**
     * 清除物品缓存
     */
    public static void clearCache() {
        allItemsCache = Collections.emptyList();
        itemStackInfoCache.clear();
    }

    /**
     * 获取所有物品的列表
     */
    public static List<ItemStack> getAllItems() {
        if (allItemsCache.isEmpty()) {
            synchronized (ItemUtils.class) {
                if (allItemsCache.isEmpty()) {
                    allItemsCache = buildAllItemsList();
                }
            }
        }
        return new ArrayList<>(allItemsCache);
    }

    /**
     * 构建所有物品列表
     */
    private static List<ItemStack> buildAllItemsList() {
        List<ItemStack> items = new ArrayList<>();
        Set<Item> addedItems = new HashSet<>();

        try {
            // 首先从创造模式搜索标签中获取所有物品变体
            try {
                ItemGroup searchTab = ItemGroup.TAB_SEARCH;
                if (searchTab != null) {
                    NonNullList<ItemStack> creativeItems = NonNullList.create();
                    searchTab.fillItemList(creativeItems);

                    if (CollectionUtils.isNotNullOrEmpty(creativeItems)) {
                        for (ItemStack stack : creativeItems) {
                            if (stack != null && !stack.isEmpty()) {
                                items.add(stack.copy());
                                addedItems.add(stack.getItem());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get items from creative search tab", e);
            }

            // 然后添加创造模式物品栏中其他物品组的物品
            try {
                for (ItemGroup group : ItemGroup.TABS) {
                    if (group == null || group == ItemGroup.TAB_SEARCH) continue;
                    try {
                        NonNullList<ItemStack> groupItems = NonNullList.create();
                        group.fillItemList(groupItems);
                        if (CollectionUtils.isNotNullOrEmpty(groupItems)) {
                            for (ItemStack stack : groupItems) {
                                if (stack != null && !stack.isEmpty()) {
                                    // 检查是否已存在相同的物品，不比较NBT
                                    boolean exists = items.stream().anyMatch(existing ->
                                            areItemsEqual(existing, stack));
                                    if (!exists) {
                                        items.add(stack.copy());
                                        addedItems.add(stack.getItem());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Failed to get items from item group: {}",
                                group.getDisplayName().getString(), e);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to get items from item groups", e);
            }

            // 最后确保所有注册的物品至少有一个默认堆叠
            for (Item item : ForgeRegistries.ITEMS) {
                if (item == null) continue;
                if (!addedItems.contains(item)) {
                    try {
                        ItemStack defaultStack = new ItemStack(item);
                        if (!defaultStack.isEmpty()) {
                            items.add(defaultStack);
                            addedItems.add(item);
                        }
                    } catch (Exception e) {
                        LOGGER.debug("Failed to create default stack for item: {}",
                                item.getRegistryName(), e);
                    }
                }
            }

            LOGGER.info("Built item list with {} items", items.size());
        } catch (Exception e) {
            LOGGER.error("Failed to build all items list", e);
        }
        return items;
    }

    /**
     * 获取物品堆信息
     *
     * @param stack 物品堆
     * @return 物品堆信息
     */
    private static ItemStackInfo getItemStackInfo(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemStackInfo("", "", "", Collections.emptySet());
        }

        // 使用序列化字符串作为key
        String cacheKey = serializeItemStack(stack);
        return itemStackInfoCache.computeIfAbsent(cacheKey, k -> {
            Item item = stack.getItem();
            String registry = getItemRegistryString(item).toLowerCase();
            String hoverName = getItemHoverNameString(stack).toLowerCase();
            String description = "";
            Set<String> tags = new HashSet<>();

            // 获取描述, 仅客户端
            try {
                if (Minecraft.getInstance() != null && Minecraft.getInstance().player != null) {
                    List<ITextComponent> tooltip = stack.getTooltipLines(
                            Minecraft.getInstance().player,
                            ITooltipFlag.TooltipFlags.NORMAL
                    );
                    if (CollectionUtils.isNotNullOrEmpty(tooltip)) {
                        description = tooltip.stream()
                                .skip(1)
                                .map(ITextComponent::getString)
                                .collect(Collectors.joining(" "))
                                .toLowerCase();
                    }
                }
            } catch (Throwable ignored) {
            }

            // 获取标签
            try {
                ResourceLocation itemId = item.getRegistryName();
                if (itemId != null) {
                    // 获取物品的所有标签
                    item.getTags().forEach(tag -> {
                        tags.add(tag.toString().toLowerCase());
                        tags.add(tag.getPath().toLowerCase());
                    });
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to get tags for item: {}", registry, e);
            }

            return new ItemStackInfo(registry, hoverName, description, tags);
        });
    }

    /**
     * 筛选物品列表
     *
     * @param predicate 筛选条件
     * @return 筛选后的物品列表
     */
    public static List<ItemStack> filterItems(Predicate<ItemStack> predicate) {
        if (predicate == null) return getAllItems();
        return getAllItems().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    /**
     * 检查物品堆是否匹配关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param info    物品堆叠信息
     * @param keyword 关键字
     * @return 是否匹配
     */
    private static boolean matchesKeyword(ItemStackInfo info, String keyword) {
        if (StringUtils.isNullOrEmpty(keyword)) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase().trim();
        if (lowerKeyword.isEmpty()) {
            return true;
        }

        // 检查前缀
        if (lowerKeyword.startsWith("@")) {
            // @ 搜索注册ID
            String searchTerm = lowerKeyword.substring(1).trim();
            return !searchTerm.isEmpty() && info.registry.contains(searchTerm);
        } else if (lowerKeyword.startsWith("#")) {
            // # 搜索标签
            String searchTerm = lowerKeyword.substring(1).trim();
            return !searchTerm.isEmpty() && info.tags.stream().anyMatch(tag -> tag.contains(searchTerm));
        } else if (lowerKeyword.startsWith("$")) {
            // $ 搜索描述
            String searchTerm = lowerKeyword.substring(1).trim();
            return !searchTerm.isEmpty() && StringUtils.isNotNullOrEmpty(info.description) && info.description.contains(searchTerm);
        } else {
            // 搜索所有字段
            // 搜索注册ID
            if (info.registry.contains(lowerKeyword)) {
                return true;
            }
            // 搜索显示名称
            if (info.hoverName.contains(lowerKeyword)) {
                return true;
            }
            // 搜索描述
            if (StringUtils.isNotNullOrEmpty(info.description) && info.description.contains(lowerKeyword)) {
                return true;
            }
            // 搜索标签
            if (info.tags.stream().anyMatch(tag -> tag.contains(lowerKeyword))) {
                return true;
            }
            return false;
        }
    }

    /**
     * 模糊搜索物品
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param keyword 搜索关键字
     * @return 匹配的物品列表
     */
    public static List<ItemStack> searchItems(String keyword) {
        if (StringUtils.isNullOrEmpty(keyword)) {
            return getAllItems();
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return getAllItems();
        }

        return getAllItems().stream()
                .filter(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    ItemStackInfo info = getItemStackInfo(stack);
                    return matchesKeyword(info, trimmedKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 模糊搜索物品
     * 支持多个关键字
     * <p>
     * - @：搜索注册ID
     * <p>
     * - #：搜索标签
     * <p>
     * - $：搜索描述
     *
     * @param keywords 搜索关键字数组
     * @return 匹配的物品列表
     */
    public static List<ItemStack> searchItems(String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return getAllItems();
        }

        List<String> validKeywords = Arrays.stream(keywords)
                .filter(StringUtils::isNotNullOrEmpty)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

        if (validKeywords.isEmpty()) {
            return getAllItems();
        }

        return getAllItems().stream()
                .filter(stack -> {
                    if (stack == null || stack.isEmpty()) return false;
                    ItemStackInfo info = getItemStackInfo(stack);
                    // 所有关键字都必须匹配
                    for (String keyword : validKeywords) {
                        if (!matchesKeyword(info, keyword)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据注册ID查找物品
     *
     * @param registry 注册ID字符串
     */
    public static ItemStack findItemByRegistry(String registry) {
        if (StringUtils.isNullOrEmpty(registry)) {
            return ItemStack.EMPTY;
        }

        try {
            ResourceLocation location = new ResourceLocation(registry);
            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to find item by registry name: {}", registry, e);
        }

        return ItemStack.EMPTY;
    }

    /**
     * 根据注册ID精确查找物品
     *
     * @param location 注册ID
     */
    public static ItemStack findItemByRegistry(ResourceLocation location) {
        if (location == null) {
            return ItemStack.EMPTY;
        }

        try {
            Item item = ForgeRegistries.ITEMS.getValue(location);
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to find item by registry name: {}", location, e);
        }

        return ItemStack.EMPTY;
    }

    // endregion
}
