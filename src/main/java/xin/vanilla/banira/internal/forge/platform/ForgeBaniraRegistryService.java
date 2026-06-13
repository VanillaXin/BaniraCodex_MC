package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.banira.platform.BaniraRegistryService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Forge 1.16.5 注册表访问实现；跨版本差异停留在 internal.forge。
 */
public final class ForgeBaniraRegistryService implements BaniraRegistryService {
    @Override
    public ResourceLocation blockKey(Block block) {
        return block != null && ForgeRegistries.BLOCKS != null ? ForgeRegistries.BLOCKS.getKey(block) : null;
    }

    @Override
    public Block block(ResourceLocation id) {
        return id != null ? ForgeRegistries.BLOCKS.getValue(id) : null;
    }

    @Override
    public Collection<Block> blocks() {
        if (ForgeRegistries.BLOCKS == null) return Collections.emptyList();
        List<Block> blocks = new ArrayList<>();
        ForgeRegistries.BLOCKS.forEach(blocks::add);
        return blocks;
    }

    @Override
    public ResourceLocation itemKey(Item item) {
        return item != null && ForgeRegistries.ITEMS != null ? ForgeRegistries.ITEMS.getKey(item) : null;
    }

    @Override
    public Item item(ResourceLocation id) {
        return id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
    }

    @Override
    public Collection<Item> items() {
        if (ForgeRegistries.ITEMS == null) return Collections.emptyList();
        List<Item> items = new ArrayList<>();
        ForgeRegistries.ITEMS.forEach(items::add);
        return items;
    }

    @Override
    public Collection<ResourceLocation> itemTagIds(Item item) {
        return Collections.emptyList();
    }

    @Override
    public ResourceLocation entityTypeKey(EntityType<?> entityType) {
        return entityType != null && ForgeRegistries.ENTITIES != null ? ForgeRegistries.ENTITIES.getKey(entityType) : null;
    }

    @Override
    public EntityType<?> entityType(ResourceLocation id) {
        return id != null ? ForgeRegistries.ENTITIES.getValue(id) : null;
    }

    @Override
    public Collection<EntityType<?>> entityTypes() {
        if (ForgeRegistries.ENTITIES == null) return Collections.emptyList();
        List<EntityType<?>> entityTypes = new ArrayList<>();
        ForgeRegistries.ENTITIES.forEach(entityTypes::add);
        return entityTypes;
    }

    @Override
    public ResourceLocation effectKey(Effect effect) {
        return effect != null && ForgeRegistries.POTIONS != null ? ForgeRegistries.POTIONS.getKey(effect) : null;
    }

    @Override
    public Effect effect(ResourceLocation id) {
        return id != null ? ForgeRegistries.POTIONS.getValue(id) : null;
    }

    @Override
    public Collection<Effect> effects() {
        if (ForgeRegistries.POTIONS == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        ForgeRegistries.POTIONS.forEach(effects::add);
        return effects;
    }

    @Override
    public Biome biome(ResourceLocation id) {
        return id != null ? ForgeRegistries.BIOMES.getValue(id) : null;
    }

    @Override
    public Collection<ResourceLocation> biomeIds() {
        return ForgeRegistries.BIOMES != null ? ForgeRegistries.BIOMES.getKeys() : Collections.emptySet();
    }
}
