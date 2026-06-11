package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import xin.vanilla.banira.platform.BaniraRegistryService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Forge 1.18.2 注册表适配层；公共 util 不直接碰 ForgeRegistries。
 */
public enum ForgeBaniraRegistryService implements BaniraRegistryService {
    INSTANCE;

    @Override
    public @Nullable ResourceLocation blockKey(@Nullable Block block) {
        return block != null ? ForgeRegistries.BLOCKS.getKey(block) : null;
    }

    @Override
    public @Nullable Block block(@Nullable ResourceLocation id) {
        return id != null ? ForgeRegistries.BLOCKS.getValue(id) : null;
    }

    @Override
    public @Nonnull Collection<Block> blocks() {
        return ForgeRegistries.BLOCKS.getValues();
    }

    @Override
    public @Nullable ResourceLocation itemKey(@Nullable Item item) {
        return item != null ? ForgeRegistries.ITEMS.getKey(item) : null;
    }

    @Override
    public @Nullable Item item(@Nullable ResourceLocation id) {
        return id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
    }

    @Override
    public @Nonnull Collection<Item> items() {
        return ForgeRegistries.ITEMS.getValues();
    }

    @Override
    public @Nonnull Collection<ResourceLocation> itemTagIds(@Nullable Item item) {
        if (item == null || ForgeRegistries.ITEMS.tags() == null) {
            return Collections.emptyList();
        }
        return ForgeRegistries.ITEMS.tags().getReverseTag(item)
                .map(reverseTag -> reverseTag.getTagKeys()
                        .map(TagKey::location)
                        .sorted(ResourceLocation::compareTo)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public @Nullable ResourceLocation entityTypeKey(@Nullable EntityType<?> entityType) {
        return entityType != null ? ForgeRegistries.ENTITIES.getKey(entityType) : null;
    }

    @Override
    public @Nullable EntityType<?> entityType(@Nullable ResourceLocation id) {
        return id != null ? ForgeRegistries.ENTITIES.getValue(id) : null;
    }

    @Override
    public @Nonnull Collection<EntityType<?>> entityTypes() {
        return ForgeRegistries.ENTITIES.getValues();
    }

    @Override
    public @Nullable ResourceLocation effectKey(@Nullable MobEffect effect) {
        return effect != null ? ForgeRegistries.MOB_EFFECTS.getKey(effect) : null;
    }

    @Override
    public @Nullable MobEffect effect(@Nullable ResourceLocation id) {
        return id != null ? ForgeRegistries.MOB_EFFECTS.getValue(id) : null;
    }

    @Override
    public @Nonnull Collection<MobEffect> effects() {
        return ForgeRegistries.MOB_EFFECTS.getValues();
    }

    @Override
    public @Nullable Biome biome(@Nullable ResourceLocation id) {
        return id != null ? ForgeRegistries.BIOMES.getValue(id) : null;
    }

    @Override
    public @Nonnull Collection<ResourceLocation> biomeIds() {
        return ForgeRegistries.BIOMES.getKeys();
    }
}
