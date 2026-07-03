package xin.vanilla.banira.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 兴趣点相关工具类
 */
public final class PoiUtils {
    private PoiUtils() {
    }

    public static PoiType getPoi(String id) {
        ResourceLocation loc = Identifier.id().parse(id);
        return loc != null ? getPoi(loc) : null;
    }

    public static PoiType getPoi(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).getOptional(id).orElse(null);
        }
        return null;
    }

    public static PoiType getPoi(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).getOptional(id).orElse(null);
        }
        return getPoi(id);
    }

    public static ResourceKey<PoiType> getKey(String id) {
        ResourceLocation loc = Identifier.id().parse(id);
        return loc != null ? getKey(loc) : null;
    }

    public static ResourceKey<PoiType> getKey(ResourceLocation id) {
        return id != null ? ResourceKey.create(Registry.POINT_OF_INTEREST_TYPE_REGISTRY, id) : null;
    }

    public static Optional<ResourceKey<PoiType>> getKey(PoiType poiType) {
        if (poiType == null) return Optional.empty();
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).getResourceKey(poiType);
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey<PoiType>> getKey(ServerLevel world, PoiType poiType) {
        if (poiType == null || world == null) return Optional.empty();
        return world.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).getResourceKey(poiType);
    }

    public static Optional<Holder<PoiType>> getHolder(ResourceLocation id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).getHolder(getKey(id));
        }
        return Optional.empty();
    }

    public static Optional<Holder<PoiType>> getHolder(ServerLevel world, ResourceLocation id) {
        if (id == null || world == null) return Optional.empty();
        return world.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).getHolder(getKey(id));
    }

    public static ResourceLocation getResourceLocation(PoiType poiType) {
        return getKey(poiType).map(ResourceKey::location).orElse(null);
    }

    public static TagKey<PoiType> getPoiTag(ResourceLocation id) {
        return TagKey.create(Registry.POINT_OF_INTEREST_TYPE_REGISTRY, id);
    }

    public static TagKey<PoiType> getPoiTag(String id) {
        return getPoiTag(Identifier.id().parse(id));
    }

    public static boolean hasPoi(ResourceLocation id) {
        if (id == null) return false;
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            var registry = server.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY);
            if (registry.containsKey(id)) return true;
            try {
                return registry.getTag(getPoiTag(id)).isPresent();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean hasPoi(String id) {
        return hasPoi(Identifier.id().parse(id));
    }

    public static Set<String> getAllIds() {
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * 在指定范围内查找最近的、匹配 POI 标签的方块位置。
     */
    public static WorldCoordinate findNearestPoi(ServerLevel world, WorldCoordinate start, TagKey<PoiType> poiTag, int radius) {
        return findNearestPoi(world, start, poiTag, radius, PoiManager.Occupancy.ANY);
    }

    public static WorldCoordinate findNearestPoi(ServerLevel world, WorldCoordinate start, TagKey<PoiType> poiTag, int radius, PoiManager.Occupancy occupancy) {
        if (world == null || start == null || poiTag == null) return null;
        var registry = world.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY);
        var holderSetOpt = registry.getTag(poiTag);
        return holderSetOpt.map(holders -> findNearestPoiImpl(world, start, holders, radius, occupancy)).orElse(null);
    }

    /**
     * 在指定范围内查找最近的、匹配单个 POI 类型的方块位置。
     */
    public static WorldCoordinate findNearestPoi(ServerLevel world, WorldCoordinate start, ResourceLocation poiId, int radius) {
        return findNearestPoi(world, start, poiId, radius, PoiManager.Occupancy.ANY);
    }

    public static WorldCoordinate findNearestPoi(ServerLevel world, WorldCoordinate start, ResourceLocation poiId, int radius, PoiManager.Occupancy occupancy) {
        if (world == null || start == null || poiId == null) return null;
        var holderOpt = getHolder(world, poiId);
        return holderOpt.map(holder -> findNearestPoiImpl(world, start, HolderSet.direct(holder), radius, occupancy)).orElse(null);
    }

    public static WorldCoordinate findNearestPoi(ServerLevel world, WorldCoordinate start, String poiId, int radius) {
        ResourceLocation loc = Identifier.id().parse(poiId);
        return loc != null ? findNearestPoi(world, start, loc, radius) : null;
    }

    public static WorldCoordinate findNearestPoi(ServerLevel world, WorldCoordinate start, String poiId, int radius, PoiManager.Occupancy occupancy) {
        ResourceLocation loc = Identifier.id().parse(poiId);
        return loc != null ? findNearestPoi(world, start, loc, radius, occupancy) : null;
    }

    private static WorldCoordinate findNearestPoiImpl(ServerLevel world, WorldCoordinate start, HolderSet<PoiType> holderSet, int radius, PoiManager.Occupancy occupancy) {
        var registry = world.registryAccess().registryOrThrow(Registry.POINT_OF_INTEREST_TYPE_REGISTRY);
        Predicate<PoiType> typePredicate = type -> registry.getResourceKey(type)
                .flatMap(registry::getHolder)
                .map(holderSet::contains)
                .orElse(false);
        BlockPos origin = start.toBlockPos();
        PoiManager poiManager = world.getPoiManager();
        poiManager.ensureLoadedAndValid(world, origin, radius);
        Optional<BlockPos> found = poiManager.findClosest(typePredicate, origin, radius, occupancy);
        return found.map(pos -> start.clone().x(pos.getX()).y(pos.getY()).z(pos.getZ())).orElse(null);
    }
}
