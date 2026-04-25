package xin.vanilla.banira.common.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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

    private static final List<String> DEFAULT_DIMENSION_IDS = Collections.unmodifiableList(Arrays.asList(
            World.OVERWORLD.location().toString(),
            World.NETHER.location().toString(),
            World.END.location().toString()
    ));

    /**
     * 是否已经向服务器请求过数据
     */
    private static boolean requestedData = false;


    public static RegistryKey<World> parse(String dimension) {
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, Identifier.id().parse(dimension));
    }

    public static RegistryKey<World> parse(ResourceLocation dimension) {
        return RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension);
    }

    public static ServerWorld getLevel(RegistryKey<World> dimension) {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        return server != null ? server.getLevel(dimension) : null;
    }

    public static ServerWorld getLevel(ResourceLocation dimension) {
        return getLevel(parse(dimension));
    }

    public static ServerWorld getLevel(String dimension) {
        return getLevel(parse(dimension));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return Collections.emptySet();
        Set<String> ids = new HashSet<>();
        server.levelKeys().forEach(key -> ids.add(key.location().toString()));
        return ids;
    }

    public static int getWorldMinY(World world) {
        if (world == null) return 0;
        return 0;
    }

    public static int getWorldMaxY(World world) {
        if (world == null) return 0;
        return world.getMaxBuildHeight();
    }

    public static String getDimensionId(Entity entity) {
        if (entity == null) return null;
        return getDimensionId(entity.level);
    }

    public static String getDimensionId(World world) {
        if (world == null) return null;
        return getDimensionId(world.dimension());
    }

    public static String getDimensionId(RegistryKey<World> dimension) {
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
        if (FMLEnvironment.dist == Dist.CLIENT && !requestedData && CLIENT_DIMENSION_IDS.isEmpty()) {
            requestDataFromServer();
        }
    }

    public static void requestDataFromServer() {
        if (FMLEnvironment.dist == Dist.CLIENT && !requestedData) {
            requestedData = true;
            PacketUtils.sendPacketToServer(new RequestToBoth(NetworkInit.REQUEST_DIMENSION_DATA));
            LOGGER.debug("Request dimension data from server.");
        }
    }

}
