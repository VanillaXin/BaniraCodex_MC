package xin.vanilla.banira.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 语言助手基类，实现 {@link ITranslator}。
 * <p>
 * 构造时传入带 {@link Mod} 的主类（与入口 {@code @Mod("modid")} 为同一类），modId 从注解读取，语言文件从该类所在 JAR 加载：
 * <pre>{@code
 * public final class MyModLang extends Translator {
 *     public static final MyModLang INSTANCE = new MyModLang();
 *     private MyModLang() { super(MyMod.class); }
 * }
 * }</pre>
 * 仅使用 {@link #of(String)} 时通过 {@link ModList} 解析该 mod 的主类。
 */
public class Translator implements ITranslator {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 默认语言代码
     */
    public static final String DEFAULT_LANGUAGE = "en_us";

    /**
     * modId -> instance 缓存
     */
    private static final Map<String, ITranslator> CACHE = new ConcurrentHashMap<>();

    /**
     * 所有语言配置
     */
    private static final Map<String, JsonObject> LANGUAGES = new ConcurrentHashMap<>();

    /**
     * 当前MOD的语言列表
     */
    private final Set<String> languages = new HashSet<>();

    private final String modId;

    /**
     * @param modMainClass 带 {@link Mod} 注解的 mod 主类（通常即 {@code @Mod("your_mod_id")} 所在类）
     */
    protected Translator(@NonNull Class<?> modMainClass) {
        this.modId = modIdFromModMainClass(modMainClass);
        getI18nFiles();
    }

    // region mod 主类与 modId（@Mod）

    @NonNull
    private static String modIdFromModMainClass(@NonNull Class<?> modMainClass) {
        if (BaniraPlatforms.isInstalled()) {
            return Banira.platform().modIdFromMainClass(modMainClass);
        }
        Mod mod = modMainClass.getAnnotation(Mod.class);
        if (mod == null) {
            throw new IllegalArgumentException("Class must be annotated with @Mod: " + modMainClass.getName());
        }
        String id = mod.value();
        if (StringUtils.isNullOrEmptyEx(id)) {
            throw new IllegalArgumentException("@Mod value is empty on: " + modMainClass.getName());
        }
        return id;
    }

    @NonNull
    private static Class<?> resolveModMainClassFromModList(@NonNull String modId) {
        if (BaniraPlatforms.isInstalled()) {
            return Banira.platform().modMainClass(modId);
        }
        try {
            return ModList.get().getModContainerById(modId)
                    .map(ModContainer::getMod)
                    .filter(Objects::nonNull)
                    .map(Object::getClass)
                    .orElseThrow(() -> new IllegalStateException("No loaded @Mod main class for mod id: " + modId));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to resolve @Mod main class for mod id: " + modId, t);
        }
    }

    // endregion mod 主类与 modId（@Mod）

    /**
     * 将当前实例注册到缓存（供直接 new 的子类在构造末尾调用）。
     * 不得在 {@link #of(String)} 的 computeIfAbsent 映射函数执行期间调用，否则会死锁。
     */
    protected final void registerInCache() {
        CACHE.put(modId, this);
    }

    /**
     * 按 modId 获取 LanguageHelper
     */
    public static ITranslator of(@NonNull String modId) {
        return CACHE.computeIfAbsent(modId, Translator::create);
    }

    private static Translator create(String modId) {
        return new Translator(resolveModMainClassFromModList(modId));
    }

    @Override
    public String getModId() {
        return modId;
    }

    @Override
    public String translate(@NonNull EnumI18nType type, @NonNull String key) {
        return getTranslation(getKey(type, key), getClientLanguage());
    }

    @Override
    public String translate(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode) {
        return getTranslation(getKey(type, key), languageCode);
    }

    @Override
    public String getTranslation(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode) {
        return getTranslation(getKey(type, key), languageCode);
    }

    @Override
    public String getTranslation(@NonNull String key, @NonNull String languageCode) {
        String path = key.replaceAll("\\.", "\\\\.");
        String norm = normalizeLanguageCode(languageCode);
        if (StringUtils.isNullOrEmptyEx(norm)) {
            norm = DEFAULT_LANGUAGE;
        }
        String result = lookupTranslation(path, norm);
        if (result != null) {
            return result;
        }
        Map<String, String> langKeys = new HashMap<>();
        for (String k : LANGUAGES.keySet()) {
            langKeys.put(normalizeLanguageCode(k), "");
        }
        for (String code : orderedFallbackCodesForPrimary(primaryLanguagePart(norm), norm, langKeys)) {
            result = lookupTranslation(path, code);
            if (result != null) {
                return result;
            }
        }
        result = lookupTranslation(path, DEFAULT_LANGUAGE);
        if (result != null) {
            return result;
        }
        return key;
    }

    @Nullable
    private static String lookupTranslation(@NonNull String path, @Nullable String normalizedLangCode) {
        if (StringUtils.isNullOrEmptyEx(normalizedLangCode)) {
            return null;
        }
        JsonObject lang = LANGUAGES.get(normalizedLangCode);
        if (lang == null) {
            return null;
        }
        return JsonUtils.getString(lang, path, null);
    }

    @Override
    public String getKey(@NonNull EnumI18nType type, @NonNull String key) {
        if (type == EnumI18nType.PLAIN || type == EnumI18nType.NONE) {
            return key;
        }
        return String.format("%s.%s.%s", type.name().toLowerCase(), modId, key);
    }

    /**
     * 指定语言的 JSON 中是否存在该翻译键路径
     */
    public boolean hasTranslation(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode) {
        String fullKey = getKey(type, key);
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        JsonObject lang = LANGUAGES.getOrDefault(languageCode, LANGUAGES.get(DEFAULT_LANGUAGE));
        if (lang == null) {
            return false;
        }
        String path = fullKey.replaceAll("\\.", "\\\\.");
        try {
            JsonElement el = JsonUtils.getJsonElement(lang, path);
            return el != null && !el.isJsonNull();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Component enabled(@NonNull String languageCode, boolean enabled) {
        return new ScopedComponent(modId).trans(EnumI18nType.WORD, enabled ? "enabled" : "disabled").languageCode(languageCode);
    }

    @Override
    public Component enabled(boolean enabled) {
        return new ScopedComponent(modId).trans(EnumI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    @Override
    public void loadLanguage(@NonNull String languageCode) {
    }

    /**
     * 获取 I18n 文件列表
     */
    public List<String> getI18nFiles() {
        if (languages.isEmpty()) {
            loadFromResourceManager();
        }
        return new ArrayList<>(languages);
    }

    private void loadFromResourceManager() {
        try {
            ResourceManager manager;
            if (BaniraCodex.serverInstance().val()) {
                manager = BaniraCodex.serverInstance().key().getResourceManager();
            } else {
                manager = net.minecraft.client.Minecraft.getInstance().getResourceManager();
            }
            Collection<ResourceLocation> resources = collectModLangJsonLocations(manager);
            languages.addAll(resources.stream()
                    .filter(loc -> modId.equals(loc.getNamespace()))
                    .map(loc -> {
                        String path = loc.getPath();
                        int slash = path.lastIndexOf('/');
                        String name = slash >= 0 ? path.substring(slash + 1) : path;
                        return name.replace(".json", "");
                    })
                    .collect(Collectors.toSet()));
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from ResourceManager:", e);
            languages.add(DEFAULT_LANGUAGE);
        }
    }

    private Collection<ResourceLocation> collectModLangJsonLocations(ResourceManager manager) {
        Set<ResourceLocation> result = new HashSet<>();
        Predicate<String> predicate = path -> path.endsWith(".json");
        try {
            manager.listPacks().forEach(pack -> {
                try {
                    Collection<ResourceLocation> locs = pack.getResources(PackType.CLIENT_RESOURCES, modId, "lang", Integer.MAX_VALUE, predicate);
                    result.addAll(locs);
                    for (ResourceLocation loc : locs) {
                        loadAllLanguages(PackType.CLIENT_RESOURCES, pack, loc);
                        loadAllLanguages(PackType.SERVER_DATA, pack, loc);
                    }
                } catch (Exception e) {
                    LOGGER.trace("Failed to list lang from pack {}: {}", pack.getName(), e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from resource packs:", e);
        }
        return result;
    }

    private static void loadAllLanguages(PackType packType, PackResources pack, ResourceLocation location) {
        try (InputStreamReader reader = new InputStreamReader(pack.getResource(packType, location), StandardCharsets.UTF_8)) {
            String path = location.getPath();
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            String languageCode = name.replace(".json", "").toLowerCase();

            JsonObject json = JsonUtils.parseObject(reader);
            JsonObject object = LANGUAGES.get(languageCode);
            if (object == null) {
                LANGUAGES.put(languageCode, json);
            } else {
                JsonUtils.mergeInPlace(object, json);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to load language file: {}", t.getMessage());
        }
    }

    private static void loadModLanguage(@Nonnull Class<?> clazz, @Nonnull String modId, @NonNull String languageCode) {
        try {
            String path = String.format("/assets/%s/lang/%s.json", modId, languageCode);
            try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(clazz.getResourceAsStream(path)), StandardCharsets.UTF_8)) {

                JsonObject json = JsonUtils.parseObject(reader);
                JsonObject object = LANGUAGES.get(languageCode);
                if (object == null) {
                    LANGUAGES.put(languageCode, json);
                } else {
                    JsonUtils.mergeInPlace(object, json);
                }
            }
            LOGGER.debug("Loaded language file for mod {}: {}", modId, path);
        } catch (Exception e) {
            LOGGER.debug("Failed to load language file for mod {}: {}", modId, e.getMessage());
        }
    }

    private String getLangPath() {
        return String.format("/assets/%s/lang/", modId);
    }

    private String getLangFilePath() {
        return getLangPath() + "%s.json";
    }

    // region 语言上下文（静态方法）

    /**
     * 获取客户端语言（服务端环境返回默认语言）
     */
    public static String getClientLanguage() {
        if (EnvironmentUtils.isClient()) {
            return normalizeLanguageCode(net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected().getCode());
        }
        return normalizeLanguageCode(CustomConfig.getDefaultLanguage());
    }

    /**
     * 获取服务端默认语言
     */
    public static String getServerLanguage() {
        return normalizeLanguageCode(CustomConfig.getDefaultLanguage());
    }

    /**
     * 获取服务端玩家语言
     */
    public static String getServerPlayerLanguage(ServerPlayer player) {
        return PlayerLanguageManager.get(player);
    }

    /**
     * 解析有效语言（支持 "client"、"server" 等特殊值）
     */
    public static String getValidLanguage(@Nullable Player player, @Nullable String language) {
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            return player instanceof ServerPlayer serverPlayer
                    ? getServerPlayerLanguage(serverPlayer)
                    : getClientLanguage();
        }
        if ("server".equalsIgnoreCase(language)) {
            return normalizeLanguageCode(CustomConfig.getDefaultLanguage());
        }
        return normalizeLanguageCode(language);
    }

    /**
     * 获取玩家语言
     */
    public static String getPlayerLanguage(@NonNull Player player) {
        try {
            String lang = player.isLocalPlayer()
                    ? CustomConfig.getPlayerLanguageClient(PlayerUtils.getPlayerUUIDString(player))
                    : CustomConfig.getPlayerLanguage(PlayerUtils.getPlayerUUIDString(player));
            return getValidLanguage(player, lang);
        } catch (IllegalArgumentException e) {
            return normalizeLanguageCode(CustomConfig.getDefaultLanguage());
        }
    }

    // region 硬编码多语言映射（语言代码 → 文案）

    /**
     * 规范化 Minecraft 风格语言代码
     */
    public static String normalizeLanguageCode(@Nullable String languageCode) {
        if (StringUtils.isNullOrEmptyEx(languageCode)) {
            return "";
        }
        return languageCode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * 从「语言代码 → 文案」映射中按当前语境选一条：精确匹配 → 同语族常用回退（如 {@code zh_tw} 可落到 {@code zh_cn}）→ {@code en_us}/{@code en_gb} → 任意一条。
     * <p>
     * 避免仅用 {@code en_us} 覆盖同语族变体。
     *
     * @param languageCode 期望语言，空则使用 {@link #getClientLanguage()}
     * @param texts        键为规范语言代码（建议小写+下划线），值为非空文案
     */
    @NonNull
    public static String pickLocalizedMapValue(@Nullable String languageCode, @Nullable Map<String, String> texts) {
        if (texts == null || texts.isEmpty()) {
            return "";
        }
        Map<String, String> norm = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : texts.entrySet()) {
            if (e.getKey() == null || StringUtils.isNullOrEmptyEx(e.getValue())) {
                continue;
            }
            norm.put(normalizeLanguageCode(e.getKey()), e.getValue());
        }
        if (norm.isEmpty()) {
            return "";
        }
        String desired = normalizeLanguageCode(languageCode);
        if (StringUtils.isNullOrEmptyEx(desired)) {
            desired = normalizeLanguageCode(getClientLanguage());
        }
        String hit = norm.get(desired);
        if (!StringUtils.isNullOrEmptyEx(hit)) {
            return hit;
        }
        String primary = primaryLanguagePart(desired);
        for (String code : orderedFallbackCodesForPrimary(primary, desired, norm)) {
            hit = norm.get(code);
            if (!StringUtils.isNullOrEmptyEx(hit)) {
                return hit;
            }
        }
        for (String code : Arrays.asList("en_us", "en_gb")) {
            hit = norm.get(code);
            if (!StringUtils.isNullOrEmptyEx(hit)) {
                return hit;
            }
        }
        for (String k : new TreeSet<>(norm.keySet())) {
            hit = norm.get(k);
            if (!StringUtils.isNullOrEmptyEx(hit)) {
                return hit;
            }
        }
        return "";
    }

    private static String primaryLanguagePart(String normalizedCode) {
        int u = normalizedCode.indexOf('_');
        return u < 0 ? normalizedCode : normalizedCode.substring(0, u);
    }

    private static List<String> orderedFallbackCodesForPrimary(String primary, String desired,
                                                               Map<String, String> norm) {
        List<String> ordered = new ArrayList<>();
        switch (primary) {
            case "zh":
                ordered.addAll(Arrays.asList("zh_cn", "zh_tw", "zh_hk", "zh_sg"));
                break;
            case "en":
                ordered.addAll(Arrays.asList("en_us", "en_gb"));
                break;
            case "pt":
                ordered.addAll(Arrays.asList("pt_br", "pt_pt"));
                break;
            case "es":
                ordered.addAll(Arrays.asList("es_es", "es_mx"));
                break;
            case "fr":
                ordered.addAll(Arrays.asList("fr_fr", "fr_ca"));
                break;
            case "ja":
                ordered.add("ja_jp");
                break;
            case "ko":
                ordered.add("ko_kr");
                break;
            case "ru":
                ordered.add("ru_ru");
                break;
            case "de":
                ordered.add("de_de");
                break;
            case "it":
                ordered.add("it_it");
                break;
            case "pl":
                ordered.add("pl_pl");
                break;
            default:
                break;
        }
        ordered.remove(desired);
        TreeSet<String> extras = new TreeSet<>();
        for (String k : norm.keySet()) {
            if (primaryLanguagePart(k).equals(primary) && !k.equals(desired) && !ordered.contains(k)) {
                extras.add(k);
            }
        }
        ordered.addAll(extras);
        return ordered;
    }

    // endregion 硬编码多语言映射（语言代码 → 文案）

    // endregion 语言上下文（静态方法）
}
