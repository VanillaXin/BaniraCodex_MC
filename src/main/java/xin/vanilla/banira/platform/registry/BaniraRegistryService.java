package xin.vanilla.banira.platform.registry;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;

import java.util.List;
import java.util.Set;

/**
 * Loader-neutral registry reads used when no live server registry is available.
 */
public interface BaniraRegistryService {
    Block block(ResourceLocation id);

    ResourceLocation blockId(Block block);

    List<Block> blocks();

    Biome biome(ResourceLocation id);

    Set<String> biomeIds();

    EntityType<?> entityType(ResourceLocation id);

    ResourceLocation entityTypeId(EntityType<?> entityType);

    List<EntityType<?>> entityTypes();

    Effect effect(ResourceLocation id);

    ResourceLocation effectId(Effect effect);

    List<Effect> effects();

    Item item(ResourceLocation id);

    ResourceLocation itemId(Item item);

    List<Item> items();

    Structure<?> structure(ResourceLocation id);

    Set<String> structureIds();
}
