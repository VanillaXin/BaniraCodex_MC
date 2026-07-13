package xin.vanilla.banira.internal.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.server.level.ServerLevel;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraWorldService;

import java.util.Collection;
import java.util.Set;

/**
 * Internal world facade. Biome, dimension, and structure API churn stays behind this boundary.
 */
public final class BaniraWorldAccess {
    private static final BaniraWorldService SERVICE = new FabricBaniraWorldService();

    private BaniraWorldAccess() {
    }

    public static Biome biome(ResourceLocation id) {
        return SERVICE.biome(id);
    }

    public static Biome biome(ServerLevel world, ResourceLocation id) {
        return SERVICE.biome(world, id);
    }

    public static Set<String> biomeIds() {
        return SERVICE.biomeIds();
    }

    public static StructureFeature<?> structure(ResourceLocation id) {
        return SERVICE.structure(id);
    }

    public static Set<String> structureIds() {
        return SERVICE.structureIds();
    }

    public static WorldCoordinate findNearestBiome(ServerLevel world, WorldCoordinate start, Biome biome, int radius, int minDistance) {
        return SERVICE.findNearestBiome(world, start, biome, radius, minDistance);
    }

    public static ServerLevel level(ResourceKey<Level> dimension) {
        return SERVICE.level(dimension);
    }

    public static Collection<ServerLevel> loadedServerWorlds() {
        return SERVICE.loadedServerWorlds();
    }

    public static ResourceKey<Level> dimensionKey(ResourceLocation dimension) {
        return SERVICE.dimensionKey(dimension);
    }

    public static Set<String> dimensionIds() {
        return SERVICE.dimensionIds();
    }

    public static int minBuildHeight(Level world) {
        return SERVICE.minBuildHeight(world);
    }

    public static int maxBuildHeight(Level world) {
        return SERVICE.maxBuildHeight(world);
    }

    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, StructureFeature<?> structure, int radius) {
        return SERVICE.findNearestStructure(world, start, structure, radius);
    }
}
