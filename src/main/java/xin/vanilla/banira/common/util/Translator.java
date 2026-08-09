package xin.vanilla.banira.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import xin.vanilla.banira.api.BaniraCommonSettings;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.resource.BaniraResourceAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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
 * 仅使用 {@link #of(String)} 时通过 {@link ModList} 与 {@link ModFileScanData} 扫描结果解析该 mod 的 {@code @Mod} 主类（NeoForge 中 {@code ModContainer} 不再持有 mod 实例）。
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

    private final Class<?> resourceAnchorClass;

    private final String modId;

    /**
     * @param modMainClass 带 {@link Mod} 注解的 mod 主类（通常即 {@code @Mod("your_mod_id")} 所在类）
     */
    protected Translator(@NonNull Class<?> modMainClass) {
        this(modIdFromModMainClass(modMainClass), modMainClass);
    }

    private Translator(@NonNull String modId) {
        this(modId, Translator.class);
    }

    protected Translator(@NonNull String modId, @NonNull Class<?> resourceAnchorClass) {
        if (StringUtils.isNullOrEmptyEx(modId)) {
            throw new IllegalArgumentException("modId must not be empty");
        }
        this.resourceAnchorClass = resourceAnchorClass;
        this.modId = modId;
        getI18nFiles();
    }

    // region mod 主类与 modId（@Mod）

    @NonNull
    private static String modIdFromModMainClass(@NonNull Class<?> modMainClass) {
        if (BaniraPlatforms.isInstalled()) {
            return BaniraPlatforms.get().modIdFromMainClass(modMainClass);
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
        try {
            IModInfo modInfo = ModList.get().getModContainerById(modId)
                    .orElseThrow(() -> new IllegalStateException("No mod container for id: " + modId))
                    .getModInfo();
            IModFile modFile = modInfo.getOwningFile().getFile();
            ModFileScanData scan = modFile.getScanResult();
            if (scan == null) {
                throw new IllegalStateException("No ModFileScanData for mod id: " + modId);
            }
            Type modAnnotationType = Type.getType(Mod.class);
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            for (ModFileScanData.AnnotationData ad : scan.getAnnotations()) {
                if (!modAnnotationType.equals(ad.annotationType())) {
                    continue;
                }
                if (!modIdMatchesModAnnotation(modId, ad.annotationData())) {
                    continue;
                }
                String binaryName = ad.clazz().getClassName();
                return Class.forName(binaryName, false, loader);
            }
            throw new IllegalStateException("No @Mod class in scan data for mod id: " + modId);
        } catch (IllegalStateException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load @Mod class for mod id: " + modId, e);
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to resolve @Mod main class for mod id: " + modId, t);
        }
    }

    private static boolean modIdMatchesModAnnotation(@NonNull String modId, @Nullable Map<String, ?> annotationData) {
        if (annotationData == null) {
            return false;
        }
        Object v = annotationData.get("value");
        if (v instanceof String s) {
            return modId.equals(s);
        }
        if (v instanceof String[] arr && arr.length == 1) {
            return modId.equals(arr[0]);
        }
        if (v instanceof List<?> list && list.size() == 1 && list.get(0) instanceof String s) {
            return modId.equals(s);
        }
        return false;
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
        try {
            return new Translator(resolveModMainClassFromModList(modId));
        } catch (RuntimeException e) {
            // 客户端可选 Mod 缺失时仍需保留翻译键，并接收服务端提供的回退文本。
            LOGGER.debug("Using key-only translator for mod {}: {}", modId, e.getMessage());
            return new Translator(modId);
        }
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
            loadFromClasspath();
        }
        return new ArrayList<>(languages);
    }

    private void loadFromClasspath() {
        try {
            URL url = resourceAnchorClass.getResource(getLangPath());
            if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
                loadKnownClasspathLanguage(DEFAULT_LANGUAGE);
                return;
            }
            try (Stream<Path> files = Files.list(Path.of(url.toURI()))) {
                files.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString().replace(".json", "").toLowerCase(Locale.ROOT))
                        .forEach(this::loadKnownClasspathLanguage);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from classpath for mod {}: {}", modId, e.getMessage());
            loadKnownClasspathLanguage(DEFAULT_LANGUAGE);
        }
    }

    private void loadKnownClasspathLanguage(String languageCode) {
        String normalized = normalizeLanguageCode(languageCode);
        if (StringUtils.isNullOrEmptyEx(normalized)) {
            return;
        }
        loadModLanguage(resourceAnchorClass, modId, normalized);
        languages.add(normalized);
    }

    private void loadFromResourceManager() {
        try {
            Map<String, JsonObject> loaded = BaniraResourceAccess.modLanguageFiles(modId);
            loaded.forEach((languageCode, json) -> {
                languages.add(languageCode);
                JsonObject existing = LANGUAGES.get(languageCode);
                if (existing == null) LANGUAGES.put(languageCode, json);
                else JsonUtils.mergeInPlace(existing, json);
            });
            if (loaded.isEmpty()) languages.add(DEFAULT_LANGUAGE);
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from ResourceManager:", e);
            languages.add(DEFAULT_LANGUAGE);
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
        if (FMLEnvironment.dist != null && FMLEnvironment.dist.isClient()) {
            return normalizeLanguageCode(net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected());
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
