package xin.vanilla.banira.common.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 结构相关工具类
 */
public final class StructureUtils {
    private StructureUtils() {
    }

    /**
     * 仅解析直连注册项
     */
    public static Structure getStructure(String id) {
        ResourceLocation loc = parseStructureId(id);
        return loc != null ? getStructure(loc) : null;
    }

    /**
     * 仅解析直连注册项
     */
    public static Structure getStructure(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getOptional(id).orElse(null);
        }
        return null;
    }

    public static Structure getStructure(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getOptional(id).orElse(null);
        }
        return getStructure(id);
    }

    public static ResourceKey<Structure> getKey(String id) {
        ResourceLocation loc = parseStructureId(id);
        return loc != null ? getKey(loc) : null;
    }

    public static ResourceKey<Structure> getKey(ResourceLocation id) {
        return id != null ? ResourceKey.create(Registry.STRUCTURE_REGISTRY, id) : null;
    }

    public static Optional<ResourceKey<Structure>> getKey(Structure structure) {
        if (structure == null) return Optional.empty();
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getResourceKey(structure);
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey<Structure>> getKey(ServerLevel world, Structure structure) {
        if (structure == null || world == null) return Optional.empty();
        return world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getResourceKey(structure);
    }

    public static Optional<Holder<Structure>> getHolder(ResourceLocation id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getHolder(getKey(id));
        }
        return Optional.empty();
    }

    public static Optional<Holder<Structure>> getHolder(ServerLevel world, ResourceLocation id) {
        if (id == null || world == null) return Optional.empty();
        return world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY).getHolder(getKey(id));
    }

    /**
     * 解析直连 id 或标签 id
     */
    public static Optional<HolderSet<Structure>> getHolderSet(String id) {
        ResourceLocation loc = parseStructureId(id);
        return loc != null ? getHolderSet(loc) : Optional.empty();
    }

    public static Optional<HolderSet<Structure>> getHolderSet(ResourceLocation id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            var registry = server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
            return resolveStructureHolderSet(registry, id);
        }
        return Optional.empty();
    }

    public static Optional<HolderSet<Structure>> getHolderSet(ServerLevel world, ResourceLocation id) {
        if (id == null || world == null) return Optional.empty();
        var registry = world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
        return resolveStructureHolderSet(registry, id);
    }

    public static ResourceLocation getResourceLocation(Structure structure) {
        return getKey(structure).map(ResourceKey::location).orElse(null);
    }

    public static TagKey<Structure> getStructureTag(ResourceLocation id) {
        return id != null ? TagKey.create(Registry.STRUCTURE_REGISTRY, id) : null;
    }

    public static TagKey<Structure> getStructureTag(String id) {
        ResourceLocation loc = parseStructureId(id);
        return loc != null ? getStructureTag(loc) : null;
    }

    public static boolean hasStructure(ResourceLocation id) {
        if (id == null) return false;
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            var registry = server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
            if (registry.containsKey(id)) return true;
            try {
                return registry.getTag(getStructureTag(id)).isPresent();
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean hasStructure(String id) {
        return hasStructure(parseStructureId(id));
    }

    /**
     * 返回当前已加载注册表中的全部直连结构 id，以及全部已绑定标签 id
     */
    public static Set<String> getAllIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            var registry = server.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
            Set<String> result = new HashSet<>();
            registry.keySet().stream().map(ResourceLocation::toString).forEach(result::add);
            registry.getTagNames().map(tagKey -> tagKey.location().toString()).forEach(result::add);
            return result;
        }
        return Set.of();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, TagKey<Structure> structureTag, int radius) {
        if (world == null || start == null || structureTag == null) return null;
        var registry = world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
        var holderSetOpt = registry.getTag(structureTag);
        return holderSetOpt.map(holders ->
                findNearestMapFeatureImpl(world, start, holders, radius)
        ).orElse(null);
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, ResourceLocation structureId, int radius) {
        if (world == null || start == null || structureId == null) return null;
        var registry = world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
        return resolveStructureHolderSet(registry, structureId)
                .map(holders -> findNearestMapFeatureImpl(world, start, holders, radius))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Optional<HolderSet<Structure>> resolveStructureHolderSet(Registry<Structure> registry, ResourceLocation id) {
        if (id == null) return Optional.empty();
        if (registry.containsKey(id)) {
            return registry.getHolder(ResourceKey.create(Registry.STRUCTURE_REGISTRY, id)).map(HolderSet::direct);
        }
        return (Optional<HolderSet<Structure>>) (Optional<?>) registry.getTag(getStructureTag(id));
    }

    private static WorldCoordinate findNearestMapFeatureImpl(ServerLevel world, WorldCoordinate start, HolderSet<Structure> holderSet, int radius) {
        Pair<BlockPos, Holder<Structure>> pair = world.getChunkSource().getGenerator()
                .findNearestMapStructure(world, holderSet, start.toBlockPos(), radius, true);
        if (pair != null) {
            BlockPos pos = pair.getFirst();
            if (pos != null) {
                return start.clone().x(pos.getX()).z(pos.getZ());
            }
        }
        return null;
    }

    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, String structureId, int radius) {
        ResourceLocation loc = parseStructureId(structureId);
        return loc != null ? findNearestStructure(world, start, loc, radius) : null;
    }

    private static ResourceLocation parseStructureId(String id) {
        return id == null ? null : Identifier.id().parse(id);
    }

}
