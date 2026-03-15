package xin.vanilla.banira.common.util;

import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 语言助手基类，实现 {@link ITranslator}。
 * <p>
 * 每个 Mod 只需继承此类并传入 modId 即可：
 * <pre>{@code
 * public final class MyModLang extends Translator {
 *     public static final MyModLang INSTANCE = new MyModLang();
 *     private MyModLang() { super(MyMod.MODID); }
 * }
 * }</pre>
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

    private final Map<String, JsonObject> languages = new ConcurrentHashMap<>();
    private final String modId;

    /**
     * 子类构造函数，传入 modId 即可完成初始化并自动注册
     */
    protected Translator(@NonNull String modId) {
        this.modId = modId;
        loadLanguage(DEFAULT_LANGUAGE);
        getI18nFiles().forEach(this::loadLanguage);
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
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        JsonObject lang = languages.getOrDefault(languageCode, languages.get(DEFAULT_LANGUAGE));
        return JsonUtils.getString(lang, key.replaceAll("\\.", "\\\\."), key);
    }

    @Override
    public String getKey(@NonNull EnumI18nType type, @NonNull String key) {
        if (type == EnumI18nType.PLAIN || type == EnumI18nType.NONE) {
            return key;
        }
        return String.format("%s.%s.%s", type.name().toLowerCase(), modId, key);
    }

    @Override
    public Component enabled(@NonNull String languageCode, boolean enabled) {
        return Component.trans(modId, EnumI18nType.WORD, enabled ? "enabled" : "disabled").languageCode(languageCode);
    }

    @Override
    public Component enabled(boolean enabled) {
        return Component.trans(modId, EnumI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    @Override
    public void loadLanguage(@NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        if (!languages.containsKey(languageCode)) {
            try {
                String path = String.format(getLangFilePath(), languageCode);
                try (InputStreamReader reader = new InputStreamReader(
                        Objects.requireNonNull(Translator.class.getResourceAsStream(path)), StandardCharsets.UTF_8)) {
                    JsonObject json = JsonUtils.parseObject(reader);
                    languages.put(languageCode, json);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load language file: {}", languageCode, e);
            }
        }
    }

    /**
     * 获取 I18n 文件列表
     */
    public List<String> getI18nFiles() {
        return loadFromResourceManager();
    }

    private List<String> loadFromResourceManager() {
        if (!FMLEnvironment.dist.isClient()) {
            return Collections.emptyList();
        }
        try {
            IResourceManager manager = Minecraft.getInstance().getResourceManager();
            Collection<ResourceLocation> resources = manager.listResources("lang", path -> path.endsWith(".json"));
            return resources.stream()
                    .filter(loc -> modId.equals(loc.getNamespace()))
                    .map(loc -> {
                        String path = loc.getPath();
                        int slash = path.lastIndexOf('/');
                        String name = slash >= 0 ? path.substring(slash + 1) : path;
                        return name.replace(".json", "");
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.debug("Failed to list lang from ResourceManager:", e);
            return Collections.emptyList();
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
        if (FMLEnvironment.dist.isClient()) {
            return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
        }
        return CustomConfig.getDefaultLanguage();
    }

    /**
     * 获取服务端默认语言
     */
    public static String getServerLanguage() {
        return CustomConfig.getDefaultLanguage();
    }

    /**
     * 获取服务端玩家语言
     */
    public static String getServerPlayerLanguage(ServerPlayerEntity player) {
        return player.getLanguage();
    }

    /**
     * 解析有效语言（支持 "client"、"server" 等特殊值）
     */
    public static String getValidLanguage(@Nullable PlayerEntity player, @Nullable String language) {
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            return player instanceof ServerPlayerEntity
                    ? getServerPlayerLanguage((ServerPlayerEntity) player)
                    : getClientLanguage();
        }
        if ("server".equalsIgnoreCase(language)) {
            return CustomConfig.getDefaultLanguage();
        }
        return language;
    }

    /**
     * 获取玩家语言
     */
    public static String getPlayerLanguage(@NonNull PlayerEntity player) {
        try {
            String lang = player.isLocalPlayer()
                    ? CustomConfig.getPlayerLanguageClient(PlayerUtils.getPlayerUUIDString(player))
                    : CustomConfig.getPlayerLanguage(PlayerUtils.getPlayerUUIDString(player));
            return getValidLanguage(player, lang);
        } catch (IllegalArgumentException e) {
            return CustomConfig.getDefaultLanguage();
        }
    }

    // endregion
}
