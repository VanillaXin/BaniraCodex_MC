package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.world.BaniraWorldService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Forge 1.16.5 world/registry lookups. Higher branches should replace only this adapter.
 */
public final class ForgeBaniraWorldService implements BaniraWorldService {
    @Override
    public Biome biome(ResourceLocation id) {
        if (id == null) return null;
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return ForgeRegistries.BIOMES != null ? ForgeRegistries.BIOMES.getValue(id) : null;
    }

    @Override
    public Biome biome(ServerWorld world, ResourceLocation id) {
        if (id == null) return null;
        if (world != null) {
            return world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getOptional(id).orElse(null);
        }
        return biome(id);
    }

    @Override
    public Set<String> biomeIds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server != null) {
            return server.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toSet());
        }
        if (ForgeRegistries.BIOMES == null) return Collections.emptySet();
        return ForgeRegistries.BIOMES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public Structure<?> structure(ResourceLocation id) {
        return id != null ? ForgeRegistries.STRUCTURE_FEATURES.getValue(id) : null;
    }

    @Override
    public Set<String> structureIds() {
        if (ForgeRegistries.STRUCTURE_FEATURES == null) return Collections.emptySet();
        return ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public WorldCoordinate findNearestBiome(ServerWorld world, WorldCoordinate start, Biome biome, int radius, int minDistance) {
        if (world == null || start == null || biome == null) return null;
        BlockPos pos = world.findNearestBiome(biome, start.toBlockPos(), radius, minDistance);
        return pos != null ? start.clone().x(pos.getX()).z(pos.getZ()) : null;
    }

    @Override
    public ServerWorld level(RegistryKey<World> dimension) {
        if (dimension == null) return null;
        MinecraftServer server = BaniraCodex.serverInstance().key();
        return server != null ? server.getLevel(dimension) : null;
    }

    @Override
    public Collection<ServerWorld> loadedServerWorlds() {
        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return Collections.emptyList();
        List<ServerWorld> worlds = new ArrayList<>();
        server.getAllLevels().forEach(worlds::add);
        return worlds;
    }

    @Override
    public RegistryKey<World> dimensionKey(ResourceLocation dimension) {
        return dimension != null ? RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension) : World.OVERWORLD;
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
    public int minBuildHeight(World world) {
        // 1.16.5 has no per-world min build height; newer branches map to Level#getMinBuildHeight.
        return 0;
    }

    @Override
    public int maxBuildHeight(World world) {
        return world != null ? world.getMaxBuildHeight() : 0;
    }

    @Override
    public WorldCoordinate findNearestStructure(ServerWorld world, WorldCoordinate start, Structure<?> structure, int radius) {
        if (world == null || start == null || structure == null) return null;
        BlockPos pos = world.findNearestMapFeature(structure, start.toBlockPos(), radius, true);
        return pos != null ? start.clone().x(pos.getX()).z(pos.getZ()) : null;
    }
}
