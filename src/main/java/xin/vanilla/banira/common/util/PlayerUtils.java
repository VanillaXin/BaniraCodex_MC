package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.GiveItemResult;
import xin.vanilla.banira.internal.mixin.accessors.ServerPlayerAccessor;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Accessors(fluent = true)
public final class PlayerUtils {
    private PlayerUtils() {
    }

    /**
     * 服务端：各玩家对应的<strong>远程客户端</strong>是否已声明安装该 mod（按 modid 区分）。</br>
     * Key: modid + ":" + 玩家UUID</br>
     * Value: 是否已同步数据（false 表示未同步，可由 tick 等逻辑补同步）
     */
    @Getter
    private static final Map<String, Boolean> remoteClientModInstalled = new ConcurrentHashMap<>();

    /**
     * 客户端：本连接上<strong>远程服务端</strong>是否已声明安装该 mod（按 modid 区分）。</br>
     * Key: modid + ":" + 玩家UUID</br>
     * Value: 是否已同步数据
     */
    @Getter
    private static final Map<String, Boolean> remoteServerModInstalled = new ConcurrentHashMap<>();

    private static String makeKey(String modid, String uuid) {
        return (modid != null ? modid : "") + ":" + (uuid != null ? uuid : "");
    }


    /**
     * 复制玩家客户端设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void cloneClientSettings(ServerPlayerEntity originalPlayer, ServerPlayerEntity targetPlayer) {
        ServerPlayerAccessor original = (ServerPlayerAccessor) originalPlayer;
        ServerPlayerAccessor target = (ServerPlayerAccessor) targetPlayer;

        target.banira$language(original.banira$language());
    }

    // region 玩家信息

    /**
     * 获取所有玩家
     */
    public static List<ServerPlayerEntity> getAllPlayers() {
        return BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().server().players() : java.util.Collections.emptyList();
    }

    /**
     * 获取随机玩家
     */
    public static ServerPlayerEntity getRandomPlayer() {
        try {
            List<ServerPlayerEntity> players = getAllPlayers();
            return players.get(ThreadLocalRandom.current().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        PlayerEntity randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUUID() : null;
    }

    public static UUID getPlayerUUID() {
        if (!BaniraPlatforms.isInstalled() || !BaniraPlatforms.get().isClient()) {
            return null;
        }
        return BaniraPlatforms.get().client().localPlayerUuid();
    }

    public static UUID getPlayerUUID(@Nonnull PlayerEntity player) {
        return player.getUUID();
    }

    public static String getPlayerUUIDString(@Nonnull PlayerEntity player) {
        return player.getUUID().toString();
    }

    public static ITextComponent getPlayerName(PlayerEntity player) {
        return player == null
                ? BaniraComponent.get().empty().toVanilla()
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
                ? BaniraComponent.get().empty().toVanilla()
                : player.getDisplayName();
    }

    @Nonnull
    public static String getPlayerDisplayNameString(PlayerEntity player) {
        return player == null
                ? ""
                : player.getDisplayName().getString();
    }

    @Nonnull
    public static String getPlayerNameString(UUID uuid) {
        String nameString = getPlayerNameString(getPlayerByUUID(uuid));
        if (StringUtils.isNullOrEmpty(nameString)) {
            if (BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()) {
                nameString = BaniraPlatforms.get().client().onlinePlayerName(uuid);
            }
        }
        if (StringUtils.isNullOrEmpty(nameString)) {
            nameString = BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().lastKnownUsername(uuid) : null;
        }
        if (StringUtils.isNullOrEmpty(nameString)) {
            nameString = uuid.toString();
        }
        return nameString;
    }

    /**
     * 通过UUID获取对应的玩家
     */
    @Nullable
    public static PlayerEntity getPlayerByUUID(String uuid) {
        try {
            return getPlayerByUUID(UUID.fromString(uuid));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 通过UUID获取对应的玩家
     */
    @Nullable
    public static ServerPlayerEntity getServerPlayerByUUID(UUID uuid) {
        return BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().server().player(uuid) : null;
    }

    /**
     * 通过UUID获取对应的玩家
     */
    @Nullable
    public static PlayerEntity getPlayerByUUID(UUID uuid) {
        PlayerEntity entity = getServerPlayerByUUID(uuid);
        if (entity != null) return entity;
        if (BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()) {
            entity = BaniraPlatforms.get().client().playerByUuid(uuid);
        }
        return entity;
    }

    @Nullable
    public static ResourceLocation getPlayerSkin(UUID uuid) {
        if (!BaniraPlatforms.isInstalled() || !BaniraPlatforms.get().isClient()) {
            return Identifier.id().create("minecraft", "textures/entity/steve.png");
        }
        ResourceLocation skin = BaniraPlatforms.get().client().playerSkin(uuid);
        return skin != null ? skin : Identifier.id().create("minecraft", "textures/entity/steve.png");
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
     * 服务端：设置<strong>远程客户端</strong>已声明安装该 mod 及数据同步状态。
     *
     * @param player 服务端玩家实体
     * @param synced 数据是否已同步
     */
    public static void setRemoteClientModInstalled(@Nonnull PlayerEntity player, @Nonnull String modid, boolean synced) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteClientModInstalled.put(makeKey(modid, getPlayerUUIDString(player)), synced);
    }

    /**
     * 客户端：设置<strong>远程服务端</strong>已声明安装该 mod 及数据同步状态。
     */
    public static void setRemoteServerModInstalled(@Nonnull PlayerEntity player, @Nonnull String modid, boolean synced) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteServerModInstalled.put(makeKey(modid, getPlayerUUIDString(player)), synced);
    }

    /**
     * 服务端：远程客户端是否已声明安装指定 mod。
     */
    public static boolean isRemoteClientModInstalled(@Nonnull PlayerEntity player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return false;
        return remoteClientModInstalled.containsKey(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 客户端：远程服务端是否已声明安装指定 mod。
     */
    public static boolean isRemoteServerModInstalled(@Nonnull PlayerEntity player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return false;
        return remoteServerModInstalled.containsKey(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 服务端：移除该玩家在「远程客户端 mod」侧的全部状态。
     */
    public static void removeRemoteClientDataStatus(@Nonnull PlayerEntity player) {
        String uuid = getPlayerUUIDString(player);
        remoteClientModInstalled.keySet().removeIf(key -> key.endsWith(":" + uuid));
    }

    /**
     * 客户端：移除该玩家在「远程服务端 mod」侧的全部状态。
     */
    public static void removeRemoteServerDataStatus(@Nonnull PlayerEntity player) {
        String uuid = getPlayerUUIDString(player);
        remoteServerModInstalled.keySet().removeIf(key -> key.endsWith(":" + uuid));
    }

    /**
     * 服务端：移除该玩家指定 mod 的远程客户端状态。
     */
    public static void removeRemoteClientDataStatus(@Nonnull PlayerEntity player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteClientModInstalled.remove(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 客户端：移除该玩家指定 mod 的远程服务端状态。
     */
    public static void removeRemoteServerDataStatus(@Nonnull PlayerEntity player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteServerModInstalled.remove(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 数据是否已同步（按逻辑侧读取对应 map：客户端看远程服务端，服务端看远程客户端）。
     *
     * @return 未记录对端 mod 或已同步
     */
    public static boolean isPlayerDataSynced(@Nonnull PlayerEntity player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return true;
        if (player.level.isClientSide()) {
            return remoteServerModInstalled.getOrDefault(makeKey(modid, getPlayerUUIDString(player)), true);
        }
        return remoteClientModInstalled.getOrDefault(makeKey(modid, getPlayerUUIDString(player)), true);
    }

    // endregion 玩家状态

}
