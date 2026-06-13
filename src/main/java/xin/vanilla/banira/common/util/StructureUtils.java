package xin.vanilla.banira.common.util;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.data.WorldCoordinate;
import xin.vanilla.banira.internal.forge.platform.ForgeStructureRegistryAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

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
        return id != null ? ForgeStructureRegistryAccess.structure(id) : null;
    }

    public static Set<String> getAllIds() {
        return ForgeStructureRegistryAccess.structureIds();
    }

    /**
     * 在指定范围内查找最近的结构位置
     */
    public static WorldCoordinate findNearestStructure(ServerWorld world, WorldCoordinate start, Structure<?> structure, int radius) {
        return BaniraPlatforms.isInstalled() ? BaniraPlatforms.get().world().findNearestStructure(world, start, structure, radius) : null;
    }

}
