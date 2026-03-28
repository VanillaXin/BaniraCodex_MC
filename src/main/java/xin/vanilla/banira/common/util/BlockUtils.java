package xin.vanilla.banira.common.util;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.Component;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class BlockUtils {
    private BlockUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation UNKNOWN_BLOCK = Identifier.id().create("unknown_block");

    /**
     * 所有方块缓存（注册表遍历顺序）
     */
    private static volatile List<Block> allBlocksCache = Collections.emptyList();

    /**
     * 反序列化缓存：String -&gt; BlockState
     */
    private static final ConcurrentMap<String, BlockState> deserializeCache = new ConcurrentHashMap<>();

    // region 获取方块注册信息

    /**
     * 获取方块的注册 ID
     *
     * @param block 方块
     */
    @Nullable
    public static ResourceLocation getBlockRegistry(Block block) {
        if (block == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    /**
     * 获取方块的注册 ID 字符串
     *
     * @param block 方块
     */
    public static String getBlockRegistryString(Block block) {
        ResourceLocation registryName = getBlockRegistry(block);
        return registryName != null ? registryName.toString() : UNKNOWN_BLOCK.toString();
    }

    /**
     * 获取方块状态的注册 ID（即其 {@link Block} 的注册 ID）
     *
     * @param state 方块状态
     */
    @Nullable
    public static ResourceLocation getBlockRegistry(BlockState state) {
        if (state == null) {
            return null;
        }
        return getBlockRegistry(state.getBlock());
    }

    /**
     * 获取方块状态的注册 ID 字符串
     *
     * @param state 方块状态
     */
    public static String getBlockRegistryString(BlockState state) {
        if (state == null) {
            return UNKNOWN_BLOCK.toString();
        }
        return getBlockRegistryString(state.getBlock());
    }

    // endregion

    // region 名称与翻译键

    /**
     * 获取方块名称翻译键
     *
     * @param block 方块
     */
    public static String getBlockNameKey(Block block) {
        if (block == null) {
            return "";
        }
        return block.getDescriptionId();
    }

    /**
     * 获取方块状态对应方块的名称翻译键
     *
     * @param state 方块状态
     */
    public static String getBlockNameKey(BlockState state) {
        if (state == null) {
            return "";
        }
        return getBlockNameKey(state.getBlock());
    }

    /**
     * 获取方块显示名称字符串
     *
     * @param state 方块状态
     */
    public static Component getBlockHoverName(BlockState state) {
        if (state == null) {
            return BaniraComponent.get().empty();
        }
        String key = getBlockNameKey(state);
        return BaniraComponent.get().object(net.minecraft.network.chat.Component.translatable(key));
    }

    /**
     * 获取方块显示名称字符串
     *
     * @param block 方块
     */
    public static Component getBlockHoverName(Block block) {
        if (block == null) {
            return BaniraComponent.get().empty();
        }
        String key = getBlockNameKey(block);
        return BaniraComponent.get().object(net.minecraft.network.chat.Component.translatable(key));
    }

    /**
     * 获取方块显示名称字符串
     *
     * @param block 方块
     */
    public static String getBlockHoverNameString(Block block) {
        if (block == null) {
            return "";
        }
        String key = getBlockNameKey(block);
        return net.minecraft.network.chat.Component.translatable(key).toString();
    }

    /**
     * 获取方块状态对应方块的显示名称字符串
     *
     * @param state 方块状态
     */
    public static String getBlockHoverNameString(BlockState state) {
        if (state == null) {
            return "";
        }
        return getBlockHoverNameString(state.getBlock());
    }

    // endregion

    // region 比较与空判断

    /**
     * 两 {@link BlockState} 是否相同（含属性值）
     */
    public static boolean areBlockStatesEqual(@Nullable BlockState a, @Nullable BlockState b) {
        if (a == null || b == null) {
            return false;
        }
        return a == b;
    }

    /**
     * 两 {@link Block} 是否为同一注册项
     */
    public static boolean areBlocksEqual(@Nullable Block a, @Nullable Block b) {
        if (a == null || b == null) {
            return false;
        }
        return a == b;
    }

    public static boolean isAir(Block block) {
        return block != null && block == Blocks.AIR;
    }

    public static boolean isAir(BlockState state) {
        return state != null && state.isAir();
    }

    // endregion

    // region 序列化与反序列化

    /**
     * 将方块状态序列化为与原版命令一致的字符串（含属性，如 {@code minecraft:oak_stairs[facing=north]}）。
     */
    public static String serializeBlockState(BlockState state) {
        if (state == null || state.isAir()) {
            return "";
        }
        try {
            return BlockStateParser.serialize(state);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize block state", e);
            return "";
        }
    }

    /**
     * 从命令格式字符串反序列化为 {@link BlockState}。
     */
    public static BlockState deserializeBlockState(String blockStateString) {
        if (StringUtils.isNullOrEmptyEx(blockStateString)) {
            return Blocks.AIR.defaultBlockState();
        }
        String key = blockStateString.trim();
        return deserializeCache.computeIfAbsent(key, k -> {
            try {
                StringReader reader = new StringReader(k);
                return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, true).blockState();
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize block state from string: {}", k, e);
                return Blocks.AIR.defaultBlockState();
            }
        });
    }

    @Nullable
    public static Block getBlockFromRegistry(String blockId) {
        ResourceLocation location = Identifier.id().parse(blockId);
        return getBlockFromRegistry(location);
    }

    @Nullable
    public static Block getBlockFromRegistry(@Nullable ResourceLocation location) {
        if (location == null) {
            return null;
        }
        try {
            return BuiltInRegistries.BLOCK.get(location);
        } catch (Exception e) {
            LOGGER.debug("Failed to find block by registry name: {}", location, e);
            return null;
        }
    }

    /**
     * 根据注册名得到该方块的默认 {@link BlockState}；找不到则返回空气
     */
    public static BlockState getDefaultBlockState(String registry) {
        Block block = getBlockFromRegistry(registry);
        if (block == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return block.defaultBlockState();
    }

    /**
     * 根据 {@link ResourceLocation} 得到默认 {@link BlockState}
     */
    public static BlockState getDefaultBlockState(@Nullable ResourceLocation location) {
        if (location == null) {
            return Blocks.AIR.defaultBlockState();
        }
        Block block = getBlockFromRegistry(location);
        if (block == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return block.defaultBlockState();
    }

    // endregion

    // region 所有方块

    /**
     * 清除方块缓存
     */
    public static void clearCache() {
        allBlocksCache = Collections.emptyList();
        deserializeCache.clear();
    }

    /**
     * 获取当前注册表中所有方块的快照列表
     */
    public static List<Block> getAllBlocks() {
        if (allBlocksCache.isEmpty()) {
            synchronized (BlockUtils.class) {
                if (allBlocksCache.isEmpty()) {
                    Map<ResourceLocation, Block> byId = new LinkedHashMap<>();
                    for (Block block : BuiltInRegistries.BLOCK) {
                        if (block == null) continue;
                        ResourceLocation rl = getBlockRegistry(block);
                        if (rl == null) rl = UNKNOWN_BLOCK;
                        byId.putIfAbsent(rl, block);
                    }
                    allBlocksCache = List.copyOf(byId.values());
                }
            }
        }
        return new ArrayList<>(allBlocksCache);
    }

    // endregion
}
