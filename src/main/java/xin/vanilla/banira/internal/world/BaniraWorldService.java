package xin.vanilla.banira.internal.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.server.level.ServerLevel;
import xin.vanilla.banira.common.data.WorldCoordinate;

import java.util.Collection;
import java.util.Set;

/**
 * Loader/version-neutral world lookups whose vanilla signatures change often.
 */
public interface BaniraWorldService {
    Biome biome(ResourceLocation id);

    Biome biome(ServerLevel world, ResourceLocation id);

    Set<String> biomeIds();

    StructureFeature<?> structure(ResourceLocation id);

    Set<String> structureIds();

    WorldCoordinate findNearestBiome(ServerLevel world, WorldCoordinate start, Biome biome, int radius, int minDistance);

    ServerLevel level(ResourceKey<Level> dimension);

    /**
     * 当前服务端已加载世界的快照列表；各版本的 server world 遍历差异留在 adapter 内。
     */
    Collection<ServerLevel> loadedServerWorlds();

    ResourceKey<Level> dimensionKey(ResourceLocation dimension);

    Set<String> dimensionIds();

    int minBuildHeight(Level world);

    int maxBuildHeight(Level world);

    WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, StructureFeature<?> structure, int radius);
}
