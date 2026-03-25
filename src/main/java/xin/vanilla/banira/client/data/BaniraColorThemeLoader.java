package xin.vanilla.banira.client.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.JsonUtils;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 从资源包加载 {@link BaniraColorConfig}（assets/&lt;namespace&gt;/banira/themes/&lt;season&gt;.json），
 * 根对象为日间配色；可选 {@code night} 子对象为夜间配色。
 * 资源缺失或解析失败时回退 {@link BaniraColorConfig#builtinForConcreteSeason(EnumSeason)} /
 * {@link BaniraColorConfig#builtinNightForConcreteSeason(EnumSeason)}；当前是否用夜间由 {@link BaniraGuiNightMode} 与客户端配置决定。
 * <p>
 * 通过 Forge {@link net.minecraftforge.event.AddReloadListenerEvent} 注册。
 */
@OnlyIn(Dist.CLIENT)
public final class BaniraColorThemeLoader extends ReloadListener<Void> {

    public static final BaniraColorThemeLoader INSTANCE = new BaniraColorThemeLoader();

    private static final Logger LOGGER = LogManager.getLogger();

    private volatile Map<EnumSeason, SeasonThemePair> cache = Collections.emptyMap();

    private BaniraColorThemeLoader() {
    }

    public static BaniraColorThemeLoader get() {
        return INSTANCE;
    }

    @Nonnull
    @Override
    public String getName() {
        return "banira_codex_color_themes";
    }

    @Override
    protected Void prepare(@Nonnull IResourceManager resourceManagerIn, @Nonnull IProfiler profilerIn) {
        return null;
    }

    @Override
    protected void apply(@Nonnull Void objectIn, @Nonnull IResourceManager resourceManagerIn, @Nonnull IProfiler profilerIn) {
        reloadFrom(resourceManagerIn);
    }

    private void reloadFrom(IResourceManager resourceManager) {
        EnumMap<EnumSeason, SeasonThemePair> next = new EnumMap<>(EnumSeason.class);
        for (EnumSeason s : new EnumSeason[]{EnumSeason.SPRING, EnumSeason.SUMMER, EnumSeason.AUTUMN, EnumSeason.WINTER}) {
            SeasonThemePair parsed = tryLoadSeason(resourceManager, s);
            if (parsed != null) {
                next.put(s, parsed);
            }
        }
        cache = Collections.unmodifiableMap(next);
    }

    @Nonnull
    public BaniraColorConfig resolve(@Nonnull EnumSeason concreteSeason) {
        boolean night = BaniraGuiNightMode.isActive();
        SeasonThemePair pair = cache.get(concreteSeason);
        if (pair != null) {
            return night ? pair.night : pair.day;
        }
        return night ? BaniraColorConfig.builtinNightForConcreteSeason(concreteSeason)
                : BaniraColorConfig.builtinForConcreteSeason(concreteSeason);
    }

    private static final class SeasonThemePair {
        final BaniraColorConfig day;
        final BaniraColorConfig night;

        SeasonThemePair(BaniraColorConfig day, BaniraColorConfig night) {
            this.day = day;
            this.night = night;
        }
    }

    private static SeasonThemePair tryLoadSeason(IResourceManager resourceManager, EnumSeason season) {
        ResourceLocation loc = themeJsonLocation(season);
        try {
            IResource res;
            try {
                res = resourceManager.getResource(loc);
            } catch (Exception missing) {
                return null;
            }
            try (InputStream in = res.getInputStream();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement root = JsonUtils.parseElement(reader);
                if (!root.isJsonObject()) {
                    LOGGER.warn("Theme JSON root must be object: {}", loc);
                    return null;
                }
                JsonObject rootObj = root.getAsJsonObject();
                BaniraColorConfig day = BaniraColorConfig.builtinForConcreteSeason(season);
                applyThemeJsonOverlay(day, rootObj);
                BaniraColorConfig night = BaniraColorConfig.builtinNightForConcreteSeason(season);
                JsonElement nightEl = rootObj.get("night");
                if (nightEl != null && nightEl.isJsonObject()) {
                    applyThemeJsonOverlay(night, nightEl.getAsJsonObject());
                }
                return new SeasonThemePair(day, night);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load Banira theme {}: {}", loc, e.getMessage());
            return null;
        }
    }

    static ResourceLocation themeJsonLocation(EnumSeason season) {
        String name = season.name().toLowerCase() + ".json";
        return Identifier.id().create(BaniraCodex.MODID, "banira/themes/" + name);
    }

    static void applyThemeJsonOverlay(BaniraColorConfig c, JsonObject o) {
        applyColor(o, "accent", c::accent);
        applyColor(o, "accentHover", c::accentHover);
        applyColor(o, "accentFocused", c::accentFocused);
        applyColor(o, "accentPressed", c::accentPressed);
        applyColor(o, "bgPrimary", c::bgPrimary);
        applyColor(o, "bgSecondary", c::bgSecondary);
        applyColor(o, "bgSurface", c::bgSurface);
        applyColor(o, "bgTertiary", c::bgTertiary);
        applyColor(o, "bgQuaternary", c::bgQuaternary);
        applyColor(o, "bgDisabled", c::bgDisabled);
        applyColor(o, "textPrimary", c::textPrimary);
        applyColor(o, "textSecondary", c::textSecondary);
        applyColor(o, "textHint", c::textHint);
        applyColor(o, "textDisabled", c::textDisabled);
        applyColor(o, "border", c::border);
        applyColor(o, "borderHover", c::borderHover);
        applyColor(o, "borderFocused", c::borderFocused);
        applyColor(o, "borderDisabled", c::borderDisabled);
        applyColor(o, "error", c::error);
        applyColor(o, "listItemTextOverride", c::listItemTextOverride);
        applyColor(o, "inputBgOverride", c::inputBgOverride);
        applyColor(o, "inputBgErrorOverride", c::inputBgErrorOverride);
        applyColor(o, "cursorLightMainOverride", c::cursorLightMainOverride);
        applyColor(o, "cursorDarkMainOverride", c::cursorDarkMainOverride);
        applyColor(o, "cursorLightPressedOverride", c::cursorLightPressedOverride);
        applyColor(o, "cursorDarkPressedOverride", c::cursorDarkPressedOverride);
        if (o.has("tooltipUseTexture") && !o.get("tooltipUseTexture").isJsonNull()) {
            c.tooltipUseTexture(readBoolean(o.get("tooltipUseTexture"), true));
        }
    }

    private static void applyColor(JsonObject o, String key, java.util.function.IntConsumer setter) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return;
        }
        int v = parseColorElement(o.get(key));
        if (v != 0) {
            setter.accept(v);
        }
    }

    private static boolean readBoolean(JsonElement el, boolean def) {
        if (el == null || el.isJsonNull()) {
            return def;
        }
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) {
                return p.getAsBoolean();
            }
            if (p.isString()) {
                String s = p.getAsString().trim();
                if ("true".equalsIgnoreCase(s)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(s)) {
                    return false;
                }
            }
        }
        return def;
    }

    /**
     * 解析 ARGB：支持十六进制字符串（可选 # / 0x）、十进制整数；0 表示未指定。
     */
    public static int parseColorElement(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return 0;
        }
        try {
            if (el.isJsonPrimitive()) {
                JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isNumber()) {
                    long v = p.getAsLong();
                    return (int) v;
                }
                if (p.isString()) {
                    String s = p.getAsString().trim();
                    if (s.isEmpty()) {
                        return 0;
                    }
                    if (s.startsWith("#")) {
                        s = s.substring(1);
                    } else if (s.length() > 2 && (s.startsWith("0x") || s.startsWith("0X"))) {
                        s = s.substring(2);
                    }
                    long v = Long.parseUnsignedLong(s, 16);
                    return (int) v;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Bad color JSON value: {}", el, e);
        }
        return 0;
    }
}
