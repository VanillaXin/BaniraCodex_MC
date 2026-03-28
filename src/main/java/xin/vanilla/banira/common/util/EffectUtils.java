package xin.vanilla.banira.common.util;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.Identifier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 药水效果工具类
 */
public final class EffectUtils {
    private EffectUtils() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation UNKNOWN_EFFECT = Identifier.id().create("unknown_effect");

    /**
     * 所有效果缓存
     */
    private static volatile List<MobEffect> allEffectsCache = new ArrayList<>();

    // region 获取效果信息

    /**
     * 获取效果的注册ID
     */
    @Nullable
    public static ResourceLocation getEffectRegistry(MobEffect effect) {
        if (effect == null) return null;
        return ForgeRegistries.MOB_EFFECTS.getKey(effect);
    }

    /**
     * 获取效果的注册ID字符串
     */
    public static String getEffectRegistryString(MobEffect effect) {
        ResourceLocation registryName = getEffectRegistry(effect);
        return registryName != null ? registryName.toString() : UNKNOWN_EFFECT.toString();
    }

    /**
     * 获取效果实例的注册ID
     */
    @Nullable
    public static ResourceLocation getEffectRegistry(MobEffectInstance effectInstance) {
        if (effectInstance == null) return null;
        return getEffectRegistry(effectInstance.getEffect());
    }

    /**
     * 获取效果实例的注册ID字符串
     */
    public static String getEffectRegistryString(MobEffectInstance effectInstance) {
        if (effectInstance == null) return UNKNOWN_EFFECT.toString();
        return getEffectRegistryString(effectInstance.getEffect());
    }

    /**
     * 获取效果的显示名称（翻译键）
     */
    public static String getEffectNameKey(MobEffect effect) {
        if (effect == null) return "";
        return effect.getDescriptionId();
    }

    /**
     * 获取效果的显示名称字符串
     */
    public static String getEffectDisplayName(MobEffect effect) {
        if (effect == null) return "";
        return effect.getDisplayName().getString();
    }

    /**
     * 获取效果实例的显示名称字符串
     */
    public static String getEffectDisplayName(MobEffectInstance effectInstance) {
        if (effectInstance == null) return "";
        return getEffectDisplayName(effectInstance.getEffect());
    }

    // endregion

    // region 效果比较与校验

    /**
     * 检查效果实例是否为空或无效
     */
    public static boolean isEffectNull(MobEffectInstance effectInstance) {
        return effectInstance == null || effectInstance.getEffect() == null;
    }

    /**
     * 复制效果实例
     */
    public static MobEffectInstance copyEffectInstance(MobEffectInstance effectInstance) {
        if (effectInstance == null) return null;
        return new MobEffectInstance(effectInstance);
    }

    /**
     * 复制效果实例
     */
    public static MobEffectInstance copyMobEffectInstance(MobEffectInstance effectInstance) {
        return copyEffectInstance(effectInstance);
    }

    /**
     * 复制效果实例
     */
    public static MobEffectInstance deserializeMobEffectInstance(String effectString) {
        return deserializeEffectInstance(effectString);
    }

    // endregion

    // region 序列化与反序列化

    /**
     * 将效果实例序列化为字符串
     * 格式: effect_id[duration,amplifier] 或 effect_id
     */
    public static String serializeEffectInstance(MobEffectInstance effectInstance) {
        if (isEffectNull(effectInstance)) return "";
        try {
            String id = getEffectRegistryString(effectInstance);
            int duration = effectInstance.getDuration();
            int amplifier = effectInstance.getAmplifier();
            if (duration == 0 && amplifier == 0) {
                return id;
            }
            return id + "[" + duration + "," + amplifier + "]";
        } catch (Exception e) {
            LOGGER.error("Failed to serialize effect instance", e);
            return "";
        }
    }

    /**
     * 从字符串反序列化效果实例
     * 支持格式: effect_id 或 effect_id[duration,amplifier]
     */
    public static MobEffectInstance deserializeEffectInstance(String effectString) {
        return deserializeEffectInstance(effectString, 600, 0);
    }

    /**
     * 从字符串反序列化效果实例，可指定默认持续时间和等级
     */
    public static MobEffectInstance deserializeEffectInstance(String effectString, int defaultDuration, int defaultAmplifier) {
        if (StringUtils.isNullOrEmptyEx(effectString)) {
            return new MobEffectInstance(MobEffects.LUCK, defaultDuration, defaultAmplifier);
        }
        effectString = effectString.trim();
        String idPart = effectString;
        int duration = defaultDuration;
        int amplifier = defaultAmplifier;

        int bracketStart = effectString.indexOf('[');
        if (bracketStart >= 0) {
            int bracketEnd = effectString.indexOf(']', bracketStart);
            if (bracketEnd > bracketStart) {
                idPart = effectString.substring(0, bracketStart).trim();
                String params = effectString.substring(bracketStart + 1, bracketEnd);
                String[] parts = params.split(",");
                if (parts.length >= 1 && StringUtils.isNotNullOrEmpty(parts[0])) {
                    duration = NumberUtils.toInt(parts[0].trim(), defaultDuration);
                }
                if (parts.length >= 2 && StringUtils.isNotNullOrEmpty(parts[1])) {
                    amplifier = NumberUtils.toInt(parts[1].trim(), defaultAmplifier);
                }
            }
        }

        return createEffectInstance(idPart, duration, amplifier);
    }

    /**
     * 根据效果ID创建效果实例
     */
    public static MobEffectInstance createEffectInstance(String effectId, int duration, int amplifier) {
        MobEffect effect = getEffectFromRegistry(effectId);
        if (effect == null) {
            return new MobEffectInstance(MobEffects.LUCK, duration, amplifier);
        }
        return new MobEffectInstance(effect, duration, amplifier);
    }

    // endregion

    // region 注册表查询

    @Nullable
    public static MobEffect getEffectFromRegistry(String effectId) {
        if (StringUtils.isNullOrEmptyEx(effectId)) return null;
        ResourceLocation location = Identifier.id().parse(effectId);
        return getEffectFromRegistry(location);
    }

    @Nullable
    public static MobEffect getEffectFromRegistry(ResourceLocation location) {
        if (location == null) return null;
        try {
            return ForgeRegistries.MOB_EFFECTS.getValue(location);
        } catch (Exception e) {
            LOGGER.debug("Failed to find effect by registry name: {}", location, e);
            return null;
        }
    }

    // endregion

    // region 所有效果

    /**
     * 清除效果缓存
     */
    public static void clearCache() {
        allEffectsCache = new ArrayList<>();
    }

    /**
     * 获取所有效果的列表
     */
    public static List<MobEffect> getAllEffects() {
        if (allEffectsCache.isEmpty()) {
            synchronized (EffectUtils.class) {
                if (allEffectsCache.isEmpty()) {
                    allEffectsCache = buildUniqueEffectsList();
                    LOGGER.debug("Built effect list with {} effects", allEffectsCache.size());
                }
            }
        }
        return new ArrayList<>(allEffectsCache);
    }

    /**
     * 获取玩家当前拥有的效果列表
     */
    @OnlyIn(Dist.CLIENT)
    public static List<MobEffect> getPlayerEffects() {
        List<MobEffect> result = new ArrayList<>();
        try {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Map<ResourceLocation, MobEffect> byId = new LinkedHashMap<>();
                for (MobEffect e : player.getActiveEffectsMap().keySet()) {
                    if (e == null) continue;
                    ResourceLocation rl = getEffectRegistry(e);
                    if (rl == null) rl = UNKNOWN_EFFECT;
                    byId.putIfAbsent(rl, e);
                }
                result.addAll(byId.values());
            }
        } catch (Throwable ignored) {
        }
        return result;
    }

    /**
     * 按注册 id 去重
     */
    private static List<MobEffect> buildUniqueEffectsList() {
        Map<ResourceLocation, MobEffect> byId = new LinkedHashMap<>();
        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS) {
            if (effect == null) continue;
            ResourceLocation rl = getEffectRegistry(effect);
            if (rl == null) rl = UNKNOWN_EFFECT;
            byId.putIfAbsent(rl, effect);
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * 模糊搜索效果
     *
     * @param keyword 搜索关键字（匹配注册ID和显示名称）
     * @return 匹配的效果列表
     */
    public static List<MobEffect> searchEffects(String keyword) {
        List<MobEffect> all = getAllEffects();
        if (StringUtils.isNullOrEmpty(keyword)) {
            return all;
        }
        String lowerKeyword = keyword.trim().toLowerCase();
        if (lowerKeyword.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(effect -> {
                    String registry = getEffectRegistryString(effect).toLowerCase();
                    String displayName = getEffectDisplayName(effect).toLowerCase();
                    return registry.contains(lowerKeyword) || displayName.contains(lowerKeyword);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据注册ID查找效果
     */
    public static MobEffect findEffectByRegistry(String registry) {
        if (StringUtils.isNullOrEmpty(registry)) return null;
        return getEffectFromRegistry(registry);
    }

    /**
     * 根据注册ID查找效果
     */
    public static MobEffect findEffectByRegistry(ResourceLocation location) {
        if (location == null) return null;
        return getEffectFromRegistry(location);
    }

    // endregion

}
