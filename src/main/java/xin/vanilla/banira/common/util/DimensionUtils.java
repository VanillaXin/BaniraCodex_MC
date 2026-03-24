package xin.vanilla.banira.common.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 维度相关工具类
 */
public final class DimensionUtils {
    private DimensionUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 客户端缓存的维度 ID 列表
     */
    private static final List<String> CLIENT_DIMENSION_IDS = new CopyOnWriteArrayList<>();

    private static final List<String> DEFAULT_DIMENSION_IDS = List.of(
            Level.OVERWORLD.location().toString(),
            Level.NETHER.location().toString(),
            Level.END.location().toString()
    );

    /**
     * 是否已经向服务器请求过数据
     */
    private static boolean requestedData = false;


    public static ResourceKey<Level> parse(String dimension) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, Identifier.id().parse(dimension));
    }

    public static ResourceKey<Level> parse(ResourceLocation dimension) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension);
    }

    public static ServerLevel getLevel(ResourceKey<Level> dimension) {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        return server != null ? server.getLevel(dimension) : null;
    }

    public static ServerLevel getLevel(ResourceLocation dimension) {
        return getLevel(parse(dimension));
    }

    public static ServerLevel getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return Collections.emptySet();
        Set<String> ids = new HashSet<>();
        server.levelKeys().forEach(key -> ids.add(key.location().toString()));
        return ids;
    }

    public static int getWorldMinY(Level world) {
        return 0;
    }

    public static int getWorldMaxY(Level world) {
        return world.getMaxBuildHeight();
    }


    /**
     * 获取客户端缓存的维度 ID 列表
     * <p>
     * 若需使用该方法，请确保已提前调用 ensureData() 方法
     */
    public static List<String> getClientDimensionIds() {
        if (CLIENT_DIMENSION_IDS.isEmpty()) return new ArrayList<>(DEFAULT_DIMENSION_IDS);
        return new ArrayList<>(CLIENT_DIMENSION_IDS);
    }

    public static void setClientDimensionIds(List<String> ids) {
        CLIENT_DIMENSION_IDS.clear();
        if (ids != null && !ids.isEmpty()) CLIENT_DIMENSION_IDS.addAll(ids);
    }

    public static void ensureData() {
        if (FMLEnvironment.dist == Dist.CLIENT && !requestedData && CLIENT_DIMENSION_IDS.isEmpty()) {
            requestDataFromServer();
        }
    }

    public static void requestDataFromServer() {
        if (FMLEnvironment.dist == Dist.CLIENT && !requestedData) {
            requestedData = true;
            PacketUtils.sendPacketToServer(NetworkInit.HANDLER.getChannel(),
                    new RequestToBoth(NetworkInit.REQUEST_DIMENSION_DATA));
            LOGGER.debug("Request dimension data from server.");
        }
    }

}
