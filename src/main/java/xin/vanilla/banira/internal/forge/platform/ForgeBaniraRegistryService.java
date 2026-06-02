package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.potion.Effect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.banira.platform.registry.BaniraRegistryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ForgeBaniraRegistryService implements BaniraRegistryService {
    @Override
    public Block block(ResourceLocation id) {
        return id != null ? ForgeRegistries.BLOCKS.getValue(id) : null;
    }

    @Override
    public ResourceLocation blockId(Block block) {
        return block != null && ForgeRegistries.BLOCKS != null ? ForgeRegistries.BLOCKS.getKey(block) : null;
    }

    @Override
    public List<Block> blocks() {
        if (ForgeRegistries.BLOCKS == null) return Collections.emptyList();
        List<Block> blocks = new ArrayList<>();
        ForgeRegistries.BLOCKS.forEach(blocks::add);
        return blocks;
    }

    @Override
    public Biome biome(ResourceLocation id) {
        return id != null ? ForgeRegistries.BIOMES.getValue(id) : null;
    }

    @Override
    public Set<String> biomeIds() {
        if (ForgeRegistries.BIOMES == null) return Collections.emptySet();
        return ForgeRegistries.BIOMES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }

    @Override
    public EntityType<?> entityType(ResourceLocation id) {
        return id != null ? ForgeRegistries.ENTITIES.getValue(id) : null;
    }

    @Override
    public ResourceLocation entityTypeId(EntityType<?> entityType) {
        return entityType != null && ForgeRegistries.ENTITIES != null ? ForgeRegistries.ENTITIES.getKey(entityType) : null;
    }

    @Override
    public List<EntityType<?>> entityTypes() {
        if (ForgeRegistries.ENTITIES == null) return Collections.emptyList();
        List<EntityType<?>> entityTypes = new ArrayList<>();
        ForgeRegistries.ENTITIES.forEach(entityTypes::add);
        return entityTypes;
    }

    @Override
    public Effect effect(ResourceLocation id) {
        return id != null ? ForgeRegistries.POTIONS.getValue(id) : null;
    }

    @Override
    public ResourceLocation effectId(Effect effect) {
        return effect != null && ForgeRegistries.POTIONS != null ? ForgeRegistries.POTIONS.getKey(effect) : null;
    }

    @Override
    public List<Effect> effects() {
        if (ForgeRegistries.POTIONS == null) return Collections.emptyList();
        List<Effect> effects = new ArrayList<>();
        ForgeRegistries.POTIONS.forEach(effects::add);
        return effects;
    }

    @Override
    public Item item(ResourceLocation id) {
        return id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
    }

    @Override
    public ResourceLocation itemId(Item item) {
        return item != null && ForgeRegistries.ITEMS != null ? ForgeRegistries.ITEMS.getKey(item) : null;
    }

    @Override
    public List<Item> items() {
        if (ForgeRegistries.ITEMS == null) return Collections.emptyList();
        List<Item> items = new ArrayList<>();
        ForgeRegistries.ITEMS.forEach(items::add);
        return items;
    }

    @Override
    public Structure<?> structure(ResourceLocation id) {
        return id != null ? ForgeRegistries.STRUCTURE_FEATURES.getValue(id) : null;
    }

    @Override
    public Set<String> structureIds() {
        if (ForgeRegistries.STRUCTURE_FEATURES == null) return Collections.emptySet();
        return ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }
}
