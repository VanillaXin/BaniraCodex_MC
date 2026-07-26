package xin.vanilla.banira.common.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.network.packet.RequestToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.internal.world.BaniraWorldAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

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

    private static final List<String> DEFAULT_DIMENSION_IDS = Collections.unmodifiableList(Arrays.asList(
            Level.OVERWORLD.location().toString(),
            Level.NETHER.location().toString(),
            Level.END.location().toString()
    ));

    /**
     * 是否已经向服务器请求过数据
     */
    private static boolean requestedData = false;


    public static ResourceKey<Level> parse(String dimension) {
        return parse(Identifier.id().parse(dimension));
    }

    public static ResourceKey<Level> parse(ResourceLocation dimension) {
        return dimension != null && BaniraPlatforms.isInstalled() ? BaniraWorldAccess.dimensionKey(dimension) : Level.OVERWORLD;
    }

    public static ServerLevel getLevel(ResourceKey<Level> dimension) {
        return dimension != null && BaniraPlatforms.isInstalled() ? BaniraWorldAccess.level(dimension) : null;
    }

    public static ServerLevel getLevel(ResourceLocation dimension) {
        return getLevel(parse(dimension));
    }

    public static ServerLevel getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

    public static Set<String> getAllIds() {
        return BaniraPlatforms.isInstalled() ? BaniraWorldAccess.dimensionIds() : Collections.emptySet();
    }

    public static int getWorldMinY(Level world) {
        return BaniraPlatforms.isInstalled() ? BaniraWorldAccess.minBuildHeight(world) : 0;
    }

    public static int getWorldMaxY(Level world) {
        return BaniraPlatforms.isInstalled() ? BaniraWorldAccess.maxBuildHeight(world) : 0;
    }

    public static String getDimensionId(Entity entity) {
        if (entity == null) return null;
        return getDimensionId(entity.level);
    }

    public static String getDimensionId(Level world) {
        if (world == null) return null;
        return getDimensionId(world.dimension());
    }

    public static String getDimensionId(ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        return dimension.location().toString();
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
        if (EnvironmentUtils.isClient() && !requestedData && CLIENT_DIMENSION_IDS.isEmpty()) {
            requestDataFromServer();
        }
    }

    public static void requestDataFromServer() {
        if (EnvironmentUtils.isClient() && !requestedData) {
            requestedData = true;
            PacketUtils.sendPacketToServer(new RequestToBoth(NetworkInit.REQUEST_DIMENSION_DATA));
            LOGGER.debug("Request dimension data from server.");
        }
    }

}
