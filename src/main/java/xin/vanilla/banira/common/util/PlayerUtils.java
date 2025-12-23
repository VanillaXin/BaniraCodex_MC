package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.GiveItemResult;
import xin.vanilla.banira.internal.mixin.accessors.ServerPlayerAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(fluent = true)
public final class PlayerUtils {
    private PlayerUtils() {
    }

    /**
     * 已安装mod的玩家列表</br>
     * 玩家UUID:是否已同步数据</br>
     * 在该map的玩家都为已安装mod</br>
     * 布尔值为false时为未同步数据，将会在玩家tick事件中检测并同步数据
     */
    @Getter
    private static final Map<String, Boolean> playerDataStatus = new ConcurrentHashMap<>();


    /**
     * 复制玩家客户端设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void cloneClientSettings(ServerPlayerEntity originalPlayer, ServerPlayerEntity targetPlayer) {
        ServerPlayerAccessor original = (ServerPlayerAccessor) originalPlayer;
        ServerPlayerAccessor target = (ServerPlayerAccessor) targetPlayer;

        target.language(original.language());
    }

    // region 玩家信息

    public static UUID getPlayerUUID(@Nonnull PlayerEntity player) {
        return player.getUUID();
    }

    public static String getPlayerUUIDString(@Nonnull PlayerEntity player) {
        return player.getUUID().toString();
    }

    public static ITextComponent getPlayerName(PlayerEntity player) {
        return player == null
                ? Component.empty().toTextComponent()
                : player.getName();
    }

    @Nonnull
    public static String getPlayerNameString(PlayerEntity player) {
        return player == null
                ? ""
                : player.getName().getString();
    }

    @Nonnull
    public static ITextComponent getPlayerDisplayName(PlayerEntity player) {
        return player == null
                ? Component.empty().toTextComponent()
                : player.getDisplayName();
    }

    @Nonnull
    public static String getPlayerDisplayNameString(PlayerEntity player) {
        return player == null
                ? ""
                : player.getDisplayName().getString();
    }

    @Nullable
    public static ServerPlayerEntity getPlayerByUUID(String uuid) {
        return StringUtils.isNullOrEmptyEx(uuid)
                ? null
                : BaniraCodex.serverInstance().key().getPlayerList().getPlayer(UUID.fromString(uuid));
    }

    // endregion 玩家信息

    // region 玩家物品管理

    /**
     * 获取玩家身上的所有物品
     *
     * @param player 玩家
     * @return 玩家身上的所有物品列表副本
     * @deprecated Use {@link ItemUtils#getAllPlayerItems}
     */
    @Deprecated
    @Nonnull
    public static List<ItemStack> getAllPlayerItems(@Nonnull PlayerEntity player) {
        return ItemUtils.getAllPlayerItems(player);
    }

    /**
     * 移除玩家身上的某个指定物品
     * <p>
     * 不比较NBT
     *
     * @param player    玩家
     * @param itemStack 物品
     * @return 实际移除的物品数量
     */
    public static int removePlayerItem(@Nonnull PlayerEntity player, @Nonnull ItemStack itemStack) {
        return removePlayerItem(player, itemStack, false);
    }

    /**
     * 移除玩家身上的某个指定物品
     *
     * @param player     玩家
     * @param itemStack  物品
     * @param compareNBT 是否比较NBT
     * @return 实际移除的物品数量
     */
    public static int removePlayerItem(@Nonnull PlayerEntity player, @Nonnull ItemStack itemStack, boolean compareNBT) {
        if (itemStack.isEmpty()) {
            return 0;
        }
        PlayerInventory inventory = player.inventory;
        if (inventory == null) {
            return 0;
        }
        int removedCount = 0;
        int targetCount = itemStack.getCount();
        // 遍历所有槽位
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                boolean matches;
                if (compareNBT) {
                    matches = ItemUtils.areItemsEqualWithNBT(stack, itemStack);
                } else {
                    matches = ItemUtils.areItemsEqual(stack, itemStack);
                }
                if (matches) {
                    int stackCount = stack.getCount();
                    int removeAmount = Math.min(stackCount, targetCount - removedCount);
                    if (removeAmount >= stackCount) {
                        // 完全移除该槽位的物品
                        inventory.setItem(i, ItemStack.EMPTY);
                        removedCount += stackCount;
                    } else {
                        // 部分移除
                        stack.shrink(removeAmount);
                        removedCount += removeAmount;
                    }
                    if (removedCount >= targetCount) {
                        break;
                    }
                }
            }
        }
        return removedCount;
    }

    /**
     * 移除玩家身上的某个指定物品
     *
     * @param player 玩家
     * @param item   物品类型
     * @param count  数量
     * @return 实际移除的物品数量
     */
    public static int removePlayerItem(@Nonnull PlayerEntity player, @Nonnull Item item, int count) {
        if (count <= 0) {
            return 0;
        }
        return removePlayerItem(player, new ItemStack(item, count));
    }

    /**
     * 判断玩家身上是否有某个指定物品
     * <p>
     * 不比较NBT
     *
     * @param player    玩家
     * @param itemStack 要检查的物品
     */
    public static boolean hasPlayerItem(@Nonnull PlayerEntity player, @Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        PlayerInventory inventory = player.inventory;
        if (inventory == null) {
            return false;
        }
        // 遍历所有槽位查找匹配的物品
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemUtils.areItemsEqual(stack, itemStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断玩家身上是否有某个指定物品
     *
     * @param player 玩家
     * @param item   物品类型
     * @param count  最小数量
     */
    public static boolean hasPlayerItem(@Nonnull PlayerEntity player, @Nonnull Item item, int count) {
        if (count <= 0) {
            return false;
        }
        PlayerInventory inventory = player.inventory;
        if (inventory == null) {
            return false;
        }
        int totalCount = 0;
        // 遍历所有槽位
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                totalCount += stack.getCount();
                if (totalCount >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断玩家身上是否有某个指定物品
     *
     * @param player 玩家
     * @param item   物品类型\
     */
    public static boolean hasPlayerItem(@Nonnull PlayerEntity player, @Nonnull Item item) {
        return hasPlayerItem(player, item, 1);
    }

    /**
     * 给予玩家物品
     *
     * @param player         玩家
     * @param itemStack      物品
     * @param dropOnGround   若背包空间不足，是否将溢出物品丢弃
     * @param abortIfNoSpace 若背包空间不足且不丢弃，是否放弃添加物品
     * @return 给予结果
     */
    @Nonnull
    public static GiveItemResult givePlayerItem(@Nonnull PlayerEntity player, @Nonnull ItemStack itemStack, boolean dropOnGround, boolean abortIfNoSpace) {
        if (itemStack.isEmpty()) {
            return GiveItemResult.success(0, 0);
        }
        PlayerInventory inventory = player.inventory;
        if (inventory == null) {
            return GiveItemResult.failure(0, 0, itemStack.getCount());
        }
        ItemStack toAdd = itemStack.copy();
        int originalCount = toAdd.getCount();
        // 尝试添加至背包
        boolean allAdded = inventory.add(toAdd);
        if (allAdded) {
            return GiveItemResult.success(originalCount, 0);
        }
        // 剩余物品
        int remainingCount = toAdd.getCount();
        int addedCount = originalCount - remainingCount;

        if (abortIfNoSpace && !dropOnGround) {
            // 放弃添加并回退已添加物品
            if (addedCount > 0) {
                ItemStack toRemove = itemStack.copy();
                toRemove.setCount(addedCount);
                removePlayerItem(player, toRemove, false);
            }
            return GiveItemResult.failure(0, 0, originalCount);
        }
        if (dropOnGround) {
            // 将剩余物品丢弃在玩家脚下
            ItemStack toDrop = itemStack.copy();
            toDrop.setCount(remainingCount);
            ItemEntity itemEntity = player.drop(toDrop, false);
            int droppedCount = 0;
            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setThrower(player.getUUID());
                droppedCount = remainingCount;
            }
            return GiveItemResult.success(addedCount, droppedCount);
        }
        return GiveItemResult.success(addedCount, 0, remainingCount);
    }

    /**
     * 给予玩家物品
     * <p>
     * 默认不丢弃溢出物品，空间不足时放弃添加
     *
     * @param player    玩家
     * @param itemStack 要给予的物品
     * @return 给予物品结果
     */
    @Nonnull
    public static GiveItemResult givePlayerItem(@Nonnull PlayerEntity player, @Nonnull ItemStack itemStack) {
        return givePlayerItem(player, itemStack, false, true);
    }

    /**
     * 给予玩家物品
     *
     * @param player         玩家
     * @param item           要给予的物品类型
     * @param count          数量
     * @param dropOnGround   若背包空间不足，是否将溢出物品丢弃
     * @param abortIfNoSpace 若背包空间不足且不丢弃，是否放弃添加物品
     * @return 给予物品结果
     */
    @Nonnull
    public static GiveItemResult givePlayerItem(@Nonnull PlayerEntity player, @Nonnull Item item, int count, boolean dropOnGround, boolean abortIfNoSpace) {
        if (count <= 0) {
            return GiveItemResult.success(0, 0);
        }
        return givePlayerItem(player, new ItemStack(item, count), dropOnGround, abortIfNoSpace);
    }

    /**
     * 给予玩家物品
     *
     * @param player 玩家
     * @param item   要给予的物品类型
     * @param count  数量
     * @return 给予物品结果
     */
    @Nonnull
    public static GiveItemResult givePlayerItem(@Nonnull PlayerEntity player, @Nonnull Item item, int count) {
        return givePlayerItem(player, item, count, false, true);
    }

    // endregion 玩家物品管理

    // region 玩家状态

    /**
     * 玩家是否安装mod
     */
    public static boolean isPlayerModInstalled(@Nonnull PlayerEntity player) {
        return PlayerUtils.playerDataStatus.containsKey(PlayerUtils.getPlayerUUIDString(player));
    }

    /**
     * 玩家数据是否同步</br>
     * 默认为同步
     */
    public static boolean isPlayerDataSynced(@Nonnull PlayerEntity player) {
        return PlayerUtils.playerDataStatus.getOrDefault(PlayerUtils.getPlayerUUIDString(player), true);
    }

    // endregion 玩家状态

}
