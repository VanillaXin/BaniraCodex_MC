package xin.vanilla.banira.platform.world;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.data.WorldCoordinate;

import java.util.Set;

/**
 * Loader/version-neutral world lookups whose vanilla signatures change often.
 */
public interface BaniraWorldService {
    Biome biome(ResourceLocation id);

    Biome biome(ServerWorld world, ResourceLocation id);

    Set<String> biomeIds();

    WorldCoordinate findNearestBiome(ServerWorld world, WorldCoordinate start, Biome biome, int radius, int minDistance);

    ServerWorld level(RegistryKey<World> dimension);

    RegistryKey<World> dimensionKey(ResourceLocation dimension);

    Set<String> dimensionIds();

    int minBuildHeight(World world);

    int maxBuildHeight(World world);

    WorldCoordinate findNearestStructure(ServerWorld world, WorldCoordinate start, Structure<?> structure, int radius);
}
