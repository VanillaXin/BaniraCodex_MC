package xin.vanilla.banira.common.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 生物群系相关工具类
 */
public final class BiomeUtils {
    private BiomeUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 客户端缓存的生物群系 ID 列表（由服务端在玩家请求时同步）
     */
    private static final List<String> CLIENT_BIOME_IDS = new CopyOnWriteArrayList<>();

    /**
     * 是否已经向服务器请求过数据
     */
    private static boolean requestedData = false;


    public static Biome getBiome(String id) {
        ResourceLocation loc = Identifier.id().parse(id);
        return loc != null ? getBiome(loc) : null;
    }

    public static Biome getBiome(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return ForgeRegistries.BIOMES.getValue(id);
    }

    public static Biome getBiome(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return getBiome(id);
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toSet());
        }
        return ForgeRegistries.BIOMES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    /**
     * 在指定范围内查找最近的生物群系位置
     */
    public static WorldCoordinate findNearestBiome(ServerLevel world, WorldCoordinate start, Biome biome, int radius, int minDistance) {
        if (world == null || start == null || biome == null) return null;
        var registry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        var biomeKey = registry.getResourceKey(biome).orElse(null);
        if (biomeKey == null) return null;
        int h = Math.max(1, minDistance);
        Pair<BlockPos, Holder<Biome>> nearestBiome = world.findClosestBiome3d(holder -> holder.is(biomeKey), start.toBlockPos(), radius, h, h);
        if (nearestBiome != null) {
            BlockPos pos = nearestBiome.getFirst();
            if (pos != null) {
                return start.clone().x(pos.getX()).z(pos.getZ());
            }
        }
        return null;
    }

    /**
     * 获取客户端缓存的群系 ID 列表
     * <p>
     * 若需使用该方法，请确保已提前调用 ensureData() 方法
     */
    public static List<String> getClientBiomeIds() {
        return new ArrayList<>(CLIENT_BIOME_IDS);
    }

    public static void setClientBiomeIds(List<String> ids) {
        CLIENT_BIOME_IDS.clear();
        if (ids != null && !ids.isEmpty()) CLIENT_BIOME_IDS.addAll(ids);
    }

    public static void ensureData() {
        if (FMLEnvironment.dist == Dist.CLIENT && !requestedData && CLIENT_BIOME_IDS.isEmpty()) {
            requestDataFromServer();
        }
    }

    public static void requestDataFromServer() {
        if (FMLEnvironment.dist == Dist.CLIENT && !requestedData) {
            requestedData = true;
            PacketUtils.sendPacketToServer(NetworkInit.HANDLER.getChannel(),
                    new RequestToBoth(NetworkInit.REQUEST_BIOME_DATA));
            LOGGER.debug("Request biome data from server.");
        }
    }

}
