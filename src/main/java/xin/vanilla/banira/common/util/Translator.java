package xin.vanilla.banira.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.BaniraCommonSettings;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.internal.common.ClientRuntimeBridge;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.platform.BaniraPlatforms;

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
 * 推荐显式传入 modId 与资源锚点类，避免公共 API 依赖 Forge/Fabric/NeoForge 的入口注解：
 * <pre>{@code
 * public final class MyModLang extends Translator {
 *     public static final MyModLang INSTANCE = new MyModLang();
 *     private MyModLang() { super("my_mod_id", MyModLang.class); }
 * }
 * }</pre>
 * 仅使用 {@link #of(String)} 时通过 Banira platform 解析该 mod 的主类。
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
     * @param modMainClass mod 主类或资源锚点类；modId 由当前 platform 解析
     */
    protected Translator(@NonNull Class<?> modMainClass) {
        this(resolveModId(modMainClass), modMainClass);
    }

    /**
     * @param modId               所属 mod id
     * @param resourceAnchorClass 用于从 classpath 定位 {@code /assets/<modid>/lang} 的同 jar 类
     */
    protected Translator(@NonNull String modId, @NonNull Class<?> resourceAnchorClass) {
        if (StringUtils.isNullOrEmptyEx(modId)) {
            throw new IllegalArgumentException("modId must not be empty");
        }
        this.modId = modId;
        getI18nFiles();
    }

    private Translator(@NonNull String modId) {
        this(modId, resolveModMainClass(modId));
    }

    // region mod 元数据

    @NonNull
    private static String resolveModId(@NonNull Class<?> modMainClass) {
        if (BaniraPlatforms.isInstalled()) {
            return Banira.platform().modIdFromMainClass(modMainClass);
        }
        throw new IllegalStateException("Banira platform has not been installed; cannot resolve mod id for class: " + modMainClass.getName());
    }

    @NonNull
    private static Class<?> resolveModMainClass(@NonNull String modId) {
        if (BaniraPlatforms.isInstalled()) {
            return Banira.platform().modMainClass(modId);
        }
        throw new IllegalStateException("Banira platform has not been installed; cannot resolve mod main class for mod id: " + modId);
    }

    // endregion mod 元数据

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
        return new Translator(modId);
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
        languageCode = normalizeLanguageCode(languageCode);
        String fullKey = getKey(type, key);
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
            Collection<ResourceLocation> resources = collectModLangJsonLocations();
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
        }
    }

    private Collection<ResourceLocation> collectModLangJsonLocations() {
        Set<ResourceLocation> result = new HashSet<>();
        Predicate<ResourceLocation> predicate = rl ->
                modId.equals(rl.getNamespace()) && rl.getPath().endsWith(".json");
        ResourceManager manager = getClientResourceManager();
        if (manager == null && BaniraCodex.serverInstance().val()) {
            manager = BaniraCodex.serverInstance().key().getResourceManager();
        }
        collectModLangJsonLocations(manager, predicate, result);
        return result;
    }

    @Nullable
    private static ResourceManager getClientResourceManager() {
        return ClientRuntimeBridge.resourceManager();
    }

    private static void collectModLangJsonLocations(ResourceManager manager, Predicate<ResourceLocation> predicate, Set<ResourceLocation> result) {
        try {
            Map<ResourceLocation, Resource> resources = manager.listResources("lang", predicate);
            result.addAll(resources.keySet());
            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                loadLanguageResource(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from resource packs:", e);
        }
    }

    private static void loadLanguageResource(ResourceLocation location, Resource resource) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
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

    // region 语言上下文（静态方法）

    /**
     * 获取客户端语言（服务端环境返回默认语言）
     */
    public static String getClientLanguage() {
        if (EnvironmentUtils.isClient()) {
            String language = ClientRuntimeBridge.selectedLanguageCode();
            if (StringUtils.isNotNullOrEmpty(language)) {
                return normalizeLanguageCode(language);
            }
        }
        return normalizeLanguageCode(BaniraCommonSettings.defaultLanguage());
    }

    /**
     * 获取服务端默认语言
     */
    public static String getServerLanguage() {
        return normalizeLanguageCode(BaniraCommonSettings.defaultLanguage());
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
            return normalizeLanguageCode(BaniraCommonSettings.defaultLanguage());
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
            return normalizeLanguageCode(BaniraCommonSettings.defaultLanguage());
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
            try {
                desired = normalizeLanguageCode(getClientLanguage());
            } catch (Throwable ignored) {
                desired = "";
            }
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
