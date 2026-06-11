package xin.vanilla.banira.common.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
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
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return Banira.platform().registryService().biome(id);
    }

    public static Biome getBiome(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return getBiome(id);
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toSet());
        }
        return Banira.platform().registryService().biomeIds().stream()
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
        Pair<BlockPos, Holder<Biome>> nearestBiome = world.findNearestBiome(holder -> holder.is(biomeKey), start.toBlockPos(), radius, minDistance);
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
        if (EnvironmentUtils.isClient() && !requestedData && CLIENT_BIOME_IDS.isEmpty()) {
            requestDataFromServer();
        }
    }

    public static void requestDataFromServer() {
        if (EnvironmentUtils.isClient() && !requestedData) {
            requestedData = true;
            PacketUtils.sendPacketToServer(new RequestToBoth(NetworkInit.REQUEST_BIOME_DATA));
            LOGGER.debug("Request biome data from server.");
        }
    }

}
