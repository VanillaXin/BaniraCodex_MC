package xin.vanilla.banira.common.util;

import com.google.gson.JsonObject;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 默认语言代码
     */
    private static final String DEFAULT_LANGUAGE = "en_us";

    /**
     *  modId -> instance
     */
    private static final Map<String, LanguageHelper> HELPERS = new HashMap<>();

    private final Map<String, JsonObject> LANGUAGES = new HashMap<>();
    private final String modId;

    private LanguageHelper(String modId) {
        this.modId = modId;
    }

    /**
     * 获取指定 modId 对应的 LanguageHelper
     */
    public static synchronized LanguageHelper forMod(@NonNull String modId) {
        LanguageHelper helper = HELPERS.get(modId);
        if (helper == null) {
            helper = new LanguageHelper(modId);
            helper.loadLanguage(DEFAULT_LANGUAGE);
            helper.getI18nFiles().forEach(helper::loadLanguage);
            HELPERS.put(modId, helper);
        }
        return helper;
    }

    /**
     * 初始化 LanguageHelper 实例
     */
    public static LanguageHelper init(String modId) {
        return forMod(modId);
    }

    /**
     * 加载语言文件
     */
    public void loadLanguage(@NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        if (!LANGUAGES.containsKey(languageCode)) {
            try {
                try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(LanguageHelper.class.getResourceAsStream(String.format(getLangFilePath(), languageCode))), StandardCharsets.UTF_8)) {
                    JsonObject jsonObject = JsonUtils.GSON.fromJson(reader, JsonObject.class);
                    LANGUAGES.put(languageCode, jsonObject);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load language file: {}", languageCode, e);
            }
        }
    }

    /**
     * 获取翻译文本
     */
    public String getTranslationClient(@NonNull EnumI18nType type, @NonNull String key) {
        return getTranslation(getKey(type, key), LanguageHelper.getClientLanguage());
    }

    /**
     * 获取翻译文本
     */
    public String getTranslation(@NonNull EnumI18nType type, @NonNull String key, @NonNull String languageCode) {
        return getTranslation(getKey(type, key), languageCode);
    }

    /**
     * 获取翻译文本
     */
    public String getTranslation(@NonNull String key, @NonNull String languageCode) {
        languageCode = languageCode.toLowerCase(Locale.ROOT);
        JsonObject language = LANGUAGES.getOrDefault(languageCode, LANGUAGES.get(DEFAULT_LANGUAGE));
        return JsonUtils.getString(language, key.replaceAll("\\.", "\\\\."), key);
    }

    /**
     * 获取翻译键
     */
    public String getKey(@NonNull EnumI18nType type, @NonNull String key) {
        String result;
        if (type == EnumI18nType.PLAIN || type == EnumI18nType.NONE) {
            result = key;
        } else {
            result = String.format("%s.%s.%s", type.name().toLowerCase(), modId, key);
        }
        return result;
    }

    public Component enabled(@NonNull String languageCode, boolean enabled) {
        return Component.translatable(languageCode, EnumI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    public Component enabled(boolean enabled) {
        return Component.translatable(EnumI18nType.WORD, enabled ? "enabled" : "disabled");
    }

    /**
     * 获取I18n文件列表
     */
    public List<String> getI18nFiles() {
        List<String> result = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(LanguageHelper.class.getResourceAsStream(getLangPath() + "0_i18n_files.txt")),
                StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // 将每一行添加到列表中
                if (StringUtils.isNotNullOrEmpty(line))
                    result.add(line);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get I18n file name list", e);
        }
        return result;
    }

    private String getLangPath() {
        return String.format("/assets/%s/lang/", modId);
    }

    private String getLangFilePath() {
        return String.format("%s%%s.json", getLangPath());
    }


    public static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
    }

    public static String getServerPlayerLanguage(ServerPlayerEntity player) {
        return player.getLanguage();
    }

    public static String getValidLanguage(@Nullable PlayerEntity player, @Nullable String language) {
        String result;
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            if (player instanceof ServerPlayerEntity) {
                result = LanguageHelper.getServerPlayerLanguage((ServerPlayerEntity) player);
            } else {
                result = LanguageHelper.getClientLanguage();
            }
        } else if ("server".equalsIgnoreCase(language)) {
            return CustomConfig.getDefaultLanguage();
        } else {
            result = language;
        }
        return result;
    }

    public static String getPlayerLanguage(@NonNull PlayerEntity player) {
        try {
            String language;
            if (player.isLocalPlayer()) {
                language = CustomConfig.getPlayerLanguageClient(PlayerUtils.getPlayerUUIDString(player));
            } else {
                language = CustomConfig.getPlayerLanguage(PlayerUtils.getPlayerUUIDString(player));
            }
            return LanguageHelper.getValidLanguage(player, language);
        } catch (IllegalArgumentException i) {
            return CustomConfig.getDefaultLanguage();
        }
    }

}
