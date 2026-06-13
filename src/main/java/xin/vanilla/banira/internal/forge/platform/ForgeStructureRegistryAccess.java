package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 1.16.5 的结构注册表仍是 Forge 静态注册表；不要提升到跨版本 platform API。
 */
public final class ForgeStructureRegistryAccess {
    private ForgeStructureRegistryAccess() {
    }

    public static Structure<?> structure(ResourceLocation id) {
        return id != null ? ForgeRegistries.STRUCTURE_FEATURES.getValue(id) : null;
    }

    public static Set<String> structureIds() {
        if (ForgeRegistries.STRUCTURE_FEATURES == null) return Collections.emptySet();
        return ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }
}
