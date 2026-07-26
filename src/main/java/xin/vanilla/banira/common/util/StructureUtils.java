package xin.vanilla.banira.common.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.world.BaniraWorldAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.Set;

/**
 * 结构相关工具类
 */
public final class StructureUtils {
    private StructureUtils() {
    }

    public static StructureFeature<?> getStructure(String id) {
        return getStructure(Identifier.id().parse(id));
    }

    public static StructureFeature<?> getStructure(ResourceLocation id) {
        return id != null ? BaniraWorldAccess.structure(id) : null;
    }

    public static Set<String> getAllIds() {
        return BaniraWorldAccess.structureIds();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerLevel world, WorldCoordinate start, StructureFeature<?> structure, int radius) {
        return BaniraPlatforms.isInstalled() ? BaniraWorldAccess.findNearestStructure(world, start, structure, radius) : null;
    }

}
