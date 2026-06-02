package xin.vanilla.banira.common.util;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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
        return id != null && BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().world().biome(id) : null;
    }

    public static Biome getBiome(ServerWorld world, ResourceLocation id) {
        return id != null && BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().world().biome(world, id) : null;
    }

    public static Set<String> getAllIds() {
        return BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().world().biomeIds() : Collections.emptySet();
    }

    /**
     * 在指定范围内查找最近的生物群系位置
     */
    public static WorldCoordinate findNearestBiome(ServerWorld world, WorldCoordinate start, Biome biome, int radius, int minDistance) {
        return BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().world().findNearestBiome(world, start, biome, radius, minDistance) : null;
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
