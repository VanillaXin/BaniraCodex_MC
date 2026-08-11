package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.GiveItemResult;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.internal.common.ClientRuntimeBridge;

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
    public static void cloneClientSettings(ServerPlayer originalPlayer, ServerPlayer targetPlayer) {
        PlayerLanguageManager.set(targetPlayer, PlayerLanguageManager.get(originalPlayer));
    }

    // region 玩家信息

    /**
     * 获取所有玩家
     */
    public static List<ServerPlayer> getAllPlayers() {
        return BaniraServerRuntime.players();
    }

    /**
     * 获取随机玩家
     */
    public static ServerPlayer getRandomPlayer() {
        try {
            List<ServerPlayer> players = getAllPlayers();
            return players.get(ThreadLocalRandom.current().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        Player randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUUID() : null;
    }

    public static UUID getPlayerUUID() {
        Player player = ClientRuntimeBridge.localPlayer();
        return player != null ? player.getUUID() : null;
    }

    public static UUID getPlayerUUID(@Nonnull Player player) {
        return player.getUUID();
    }

    public static String getPlayerUUIDString(@Nonnull Player player) {
        return player.getUUID().toString();
    }

    public static net.minecraft.network.chat.Component getPlayerName(Player player) {
        return player == null
                ? BaniraComponent.get().empty().toVanilla()
                : player.getName();
    }

    @Nonnull
    public static String getPlayerNameString(Player player) {
        return player == null
                ? ""
                : player.getName().getString();
    }

    @Nonnull
    public static net.minecraft.network.chat.Component getPlayerDisplayName(Player player) {
        return player == null
                ? BaniraComponent.get().empty().toVanilla()
                : player.getDisplayName();
    }

    @Nonnull
    public static String getPlayerDisplayNameString(Player player) {
        return player == null
                ? ""
                : player.getDisplayName().getString();
    }

    @Nonnull
    public static String getPlayerNameString(UUID uuid) {
        String nameString = getPlayerNameString(getPlayerByUUID(uuid));
        if (StringUtils.isNullOrEmpty(nameString)) {
            try {
                if (EnvironmentUtils.isClient()) {
                    nameString = ClientRuntimeBridge.onlinePlayerName(uuid);
                }
            } catch (Throwable ignored) {
            }
        }
        if (StringUtils.isNullOrEmpty(nameString)) {
            nameString = Banira.platform().lastKnownUsername(uuid);
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
    public static Player getPlayerByUUID(String uuid) {
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
    public static ServerPlayer getServerPlayerByUUID(UUID uuid) {
        try {
            return BaniraServerRuntime.player(uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 通过UUID获取对应的玩家
     */
    @Nullable
    public static Player getPlayerByUUID(UUID uuid) {
        Player entity = getServerPlayerByUUID(uuid);
        if (entity != null) return entity;
        try {
            entity = ClientRuntimeBridge.levelPlayer(uuid);
        } catch (Throwable ignored) {
        }
        return entity;
    }

    @Nullable
    public static ResourceLocation getPlayerSkin(UUID uuid) {
        try {
            ResourceLocation skin = ClientRuntimeBridge.onlinePlayerSkin(uuid);
            if (skin != null) return skin;
        } catch (Throwable ignored) {
        }
        return Identifier.id().create("minecraft", "textures/entity/steve.png");
    }

    // endregion 玩家信息

    // region 玩家物品管理

    /**
     * 移除玩家身上的某个指定物品
     * <p>
     * 不比较NBT
     *
     * @param player    玩家
     * @param itemStack 物品
     * @return 实际移除的物品数量
     */
    public static int removePlayerItem(@Nonnull Player player, @Nonnull ItemStack itemStack) {
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
    public static int removePlayerItem(@Nonnull Player player, @Nonnull ItemStack itemStack, boolean compareNBT) {
        if (itemStack.isEmpty()) {
            return 0;
        }
        Inventory inventory = player.getInventory();
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
    public static int removePlayerItem(@Nonnull Player player, @Nonnull Item item, int count) {
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
    public static boolean hasPlayerItem(@Nonnull Player player, @Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        Inventory inventory = player.getInventory();
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
    public static boolean hasPlayerItem(@Nonnull Player player, @Nonnull Item item, int count) {
        if (count <= 0) {
            return false;
        }
        Inventory inventory = player.getInventory();
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
    public static boolean hasPlayerItem(@Nonnull Player player, @Nonnull Item item) {
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
    public static GiveItemResult givePlayerItem(@Nonnull Player player, @Nonnull ItemStack itemStack, boolean dropOnGround, boolean abortIfNoSpace) {
        if (itemStack.isEmpty()) {
            return GiveItemResult.success(0, 0);
        }
        Inventory inventory = player.getInventory();
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
                itemEntity.setThrower(player);
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
    public static GiveItemResult givePlayerItem(@Nonnull Player player, @Nonnull ItemStack itemStack) {
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
    public static GiveItemResult givePlayerItem(@Nonnull Player player, @Nonnull Item item, int count, boolean dropOnGround, boolean abortIfNoSpace) {
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
    public static GiveItemResult givePlayerItem(@Nonnull Player player, @Nonnull Item item, int count) {
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
    public static void setRemoteClientModInstalled(@Nonnull Player player, @Nonnull String modid, boolean synced) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteClientModInstalled.put(makeKey(modid, getPlayerUUIDString(player)), synced);
    }

    /**
     * 客户端：设置<strong>远程服务端</strong>已声明安装该 mod 及数据同步状态。
     */
    public static void setRemoteServerModInstalled(@Nonnull Player player, @Nonnull String modid, boolean synced) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteServerModInstalled.put(makeKey(modid, getPlayerUUIDString(player)), synced);
    }

    /**
     * 服务端：远程客户端是否已声明安装指定 mod。
     */
    public static boolean isRemoteClientModInstalled(@Nonnull Player player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return false;
        return remoteClientModInstalled.containsKey(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 客户端：远程服务端是否已声明安装指定 mod。
     */
    public static boolean isRemoteServerModInstalled(@Nonnull Player player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return false;
        return remoteServerModInstalled.containsKey(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 服务端：移除该玩家在「远程客户端 mod」侧的全部状态。
     */
    public static void removeRemoteClientDataStatus(@Nonnull Player player) {
        String uuid = getPlayerUUIDString(player);
        remoteClientModInstalled.keySet().removeIf(key -> key.endsWith(":" + uuid));
    }

    /**
     * 客户端：移除该玩家在「远程服务端 mod」侧的全部状态。
     */
    public static void removeRemoteServerDataStatus(@Nonnull Player player) {
        String uuid = getPlayerUUIDString(player);
        remoteServerModInstalled.keySet().removeIf(key -> key.endsWith(":" + uuid));
    }

    /**
     * 服务端：移除该玩家指定 mod 的远程客户端状态。
     */
    public static void removeRemoteClientDataStatus(@Nonnull Player player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteClientModInstalled.remove(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 客户端：移除该玩家指定 mod 的远程服务端状态。
     */
    public static void removeRemoteServerDataStatus(@Nonnull Player player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return;
        remoteServerModInstalled.remove(makeKey(modid, getPlayerUUIDString(player)));
    }

    /**
     * 数据是否已同步（按逻辑侧读取对应 map：客户端看远程服务端，服务端看远程客户端）。
     *
     * @return 未记录对端 mod 或已同步
     */
    public static boolean isPlayerDataSynced(@Nonnull Player player, @Nonnull String modid) {
        if (StringUtils.isNullOrEmptyEx(modid)) return true;
        if (player.level().isClientSide()) {
            return remoteServerModInstalled.getOrDefault(makeKey(modid, getPlayerUUIDString(player)), true);
        }
        return remoteClientModInstalled.getOrDefault(makeKey(modid, getPlayerUUIDString(player)), true);
    }

    // endregion 玩家状态

}
