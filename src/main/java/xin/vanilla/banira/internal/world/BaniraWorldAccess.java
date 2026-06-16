package xin.vanilla.banira.internal.world;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraWorldService;

import java.util.Collection;
import java.util.Set;

/**
 * Internal world facade. Biome, dimension, and structure API churn stays behind this boundary.
 */
public final class BaniraWorldAccess {
    private static final BaniraWorldService SERVICE = new ForgeBaniraWorldService();

    private BaniraWorldAccess() {
    }

    public static Biome biome(ResourceLocation id) {
        return SERVICE.biome(id);
    }

    public static Biome biome(ServerWorld world, ResourceLocation id) {
        return SERVICE.biome(world, id);
    }

    public static Set<String> biomeIds() {
        return SERVICE.biomeIds();
    }

    public static WorldCoordinate findNearestBiome(ServerWorld world, WorldCoordinate start, Biome biome, int radius, int minDistance) {
        return SERVICE.findNearestBiome(world, start, biome, radius, minDistance);
    }

    public static ServerWorld level(RegistryKey<World> dimension) {
        return SERVICE.level(dimension);
    }

    public static Collection<ServerWorld> loadedServerWorlds() {
        return SERVICE.loadedServerWorlds();
    }

    public static RegistryKey<World> dimensionKey(ResourceLocation dimension) {
        return SERVICE.dimensionKey(dimension);
    }

    public static Set<String> dimensionIds() {
        return SERVICE.dimensionIds();
    }

    public static int minBuildHeight(World world) {
        return SERVICE.minBuildHeight(world);
    }

    public static int maxBuildHeight(World world) {
        return SERVICE.maxBuildHeight(world);
    }

    public static WorldCoordinate findNearestStructure(ServerWorld world, WorldCoordinate start, Structure<?> structure, int radius) {
        return SERVICE.findNearestStructure(world, start, structure, radius);
    }
}
