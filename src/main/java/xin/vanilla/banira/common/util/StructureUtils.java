package xin.vanilla.banira.common.util;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.Collections;
import java.util.Set;

/**
 * 结构相关工具类
 */
public final class StructureUtils {
    private StructureUtils() {
    }

    public static Structure<?> getStructure(String id) {
        return getStructure(Identifier.id().parse(id));
    }

    public static Structure<?> getStructure(ResourceLocation id) {
        return id != null && BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().registry().structure(id) : null;
    }

    public static Set<String> getAllIds() {
        return BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().registry().structureIds() : Collections.emptySet();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerWorld world, WorldCoordinate start, Structure<?> structure, int radius) {
        if (world == null || start == null || structure == null) return null;
        BlockPos pos = world.findNearestMapFeature(structure, start.toBlockPos(), radius, true);
        if (pos != null) {
            return start.clone().x(pos.getX()).z(pos.getZ());
        }
        return null;
    }

}
