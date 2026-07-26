package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.world.BaniraWorldService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Fabric 1.16 世界与注册表适配。
 */
public final class FabricBaniraWorldService implements BaniraWorldService {
    @Override
    public Biome biome(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null)
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        return null;
    }

    @Override
    public Biome biome(ServerLevel world, ResourceLocation id) {
        if (id == null) return null;
        return world != null ? world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null) : biome(id);
    }

    @Override
    public Set<String> biomeIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return Collections.emptySet();
        Registry<Biome> registry = server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        return registry.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toSet());
    }

    @Override
    public StructureFeature<?> structure(ResourceLocation id) {
        return id != null ? Registry.STRUCTURE_FEATURE.getOptional(id).orElse(null) : null;
    }

    @Override
    public Set<String> structureIds() {
        return Registry.STRUCTURE_FEATURE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toSet());
    }

    @Override
    public WorldCoordinate findNearestBiome(ServerLevel world, WorldCoordinate start, Biome biome, int radius, int minDistance) {
        if (world == null || start == null || biome == null) return null;
        BlockPos pos = world.findNearestBiome(biome, start.toBlockPos(), radius, minDistance);
        return pos != null ? start.clone().x(pos.getX()).z(pos.getZ()) : null;
    }

    @Override
    public ServerLevel level(ResourceKey<Level> dimension) {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        return server != null && dimension != null ? server.getLevel(dimension) : null;
    }

    @Override
    public Collection<ServerLevel> loadedServerWorlds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return Collections.emptyList();
        List<ServerLevel> worlds = new ArrayList<>();
        server.getAllLevels().forEach(worlds::add);
        return worlds;
    }

    @Override
    public ResourceKey<Level> dimensionKey(ResourceLocation dimension) {
        return dimension != null ? ResourceKey.create(Registry.DIMENSION_REGISTRY, dimension) : Level.OVERWORLD;
    }

    @Override
    public Set<String> dimensionIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return Collections.emptySet();
        Set<String> ids = new HashSet<>();
        server.levelKeys().forEach(key -> ids.add(key.location().toString()));
        return ids;
    }

    @Override
    public int minBuildHeight(Level world) {
        return 0;
    }

    @Override
    public int maxBuildHeight(Level world) {
        return world != null ? world.getMaxBuildHeight() : 0;
    }

    @Override
    public WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, StructureFeature<?> structure, int radius) {
        if (world == null || start == null || structure == null) return null;
        BlockPos pos = world.findNearestMapFeature(structure, start.toBlockPos(), radius, true);
        return pos != null ? start.clone().x(pos.getX()).z(pos.getZ()) : null;
    }
}
