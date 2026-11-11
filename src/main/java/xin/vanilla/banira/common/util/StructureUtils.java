package xin.vanilla.banira.common.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

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
        Identifier loc = parseStructureId(id);
        return loc != null ? getStructure(loc) : null;
    }

    /**
     * 仅解析直连注册项
     */
    public static Structure getStructure(Identifier id) {
        if (id == null) return null;
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOptional(id).orElse(null);
        }
        return null;
    }

    public static Structure getStructure(ServerLevel world, Identifier id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOptional(id).orElse(null);
        }
        return getStructure(id);
    }

    public static ResourceKey<Structure> getKey(String id) {
        Identifier loc = parseStructureId(id);
        return loc != null ? getKey(loc) : null;
    }

    public static ResourceKey<Structure> getKey(Identifier id) {
        return id != null ? ResourceKey.create(Registries.STRUCTURE, id) : null;
    }

    public static Optional<ResourceKey<Structure>> getKey(Structure structure) {
        if (structure == null) return Optional.empty();
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structure);
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey<Structure>> getKey(ServerLevel world, Structure structure) {
        if (structure == null || world == null) return Optional.empty();
        return world.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structure);
    }

    public static Optional<Holder.Reference<Structure>> getHolder(Identifier id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            return server.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(getKey(id));
        }
        return Optional.empty();
    }

    public static Optional<Holder.Reference<Structure>> getHolder(ServerLevel world, Identifier id) {
        if (id == null || world == null) return Optional.empty();
        return world.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(getKey(id));
    }

    /**
     * 解析直连 id 或标签 id
     */
    public static Optional<HolderSet<Structure>> getHolderSet(String id) {
        Identifier loc = parseStructureId(id);
        return loc != null ? getHolderSet(loc) : Optional.empty();
    }

    public static Optional<HolderSet<Structure>> getHolderSet(Identifier id) {
        if (id == null) return Optional.empty();
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            return resolveStructureHolderSet(registry, id);
        }
        return Optional.empty();
    }

    public static Optional<HolderSet<Structure>> getHolderSet(ServerLevel world, Identifier id) {
        if (id == null || world == null) return Optional.empty();
        var registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return resolveStructureHolderSet(registry, id);
    }

    public static Identifier getResourceLocation(Structure structure) {
        return getKey(structure).map(ResourceKey::identifier).orElse(null);
    }

    public static TagKey<Structure> getStructureTag(Identifier id) {
        return id != null ? TagKey.create(Registries.STRUCTURE, id) : null;
    }

    public static TagKey<Structure> getStructureTag(String id) {
        Identifier loc = parseStructureId(id);
        return loc != null ? getStructureTag(loc) : null;
    }

    public static boolean hasStructure(Identifier id) {
        if (id == null) return false;
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            if (registry.containsKey(id)) return true;
            try {
                return registry.get(getStructureTag(id)).isPresent();
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
        MinecraftServer server = BaniraServerRuntime.server();
        if (server != null) {
            var registry = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            Set<String> result = new HashSet<>();
            registry.keySet().stream().map(Identifier::toString).forEach(result::add);
            registry.getTags().map(HolderSet.Named::key).map(tagKey -> tagKey.location().toString()).forEach(result::add);
            return result;
        }
        return Set.of();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, TagKey<Structure> structureTag, int radius) {
        if (world == null || start == null || structureTag == null) return null;
        var registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var holderSetOpt = registry.get(structureTag);
        return holderSetOpt.map(holders ->
                findNearestMapFeatureImpl(world, start, holders, radius)
        ).orElse(null);
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, Identifier structureId, int radius) {
        if (world == null || start == null || structureId == null) return null;
        var registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return resolveStructureHolderSet(registry, structureId)
                .map(holders -> findNearestMapFeatureImpl(world, start, holders, radius))
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Optional<HolderSet<Structure>> resolveStructureHolderSet(Registry<Structure> registry, Identifier id) {
        if (id == null) return Optional.empty();
        if (registry.containsKey(id)) {
            return registry.get(ResourceKey.create(Registries.STRUCTURE, id)).map(HolderSet::direct);
        }
        return (Optional<HolderSet<Structure>>) (Optional<?>) registry.get(getStructureTag(id));
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
        Identifier loc = parseStructureId(structureId);
        return loc != null ? findNearestStructure(world, start, loc, radius) : null;
    }

    private static Identifier parseStructureId(String id) {
        return id == null ? null : xin.vanilla.banira.Identifier.id().parse(id);
    }

}
