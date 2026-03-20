package xin.vanilla.banira.common.config;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置分类（折叠面板）标题的展示元数据，与 {@link xin.vanilla.banira.common.config.annotation.ConfigEntry.Gui.Tooltip} 解析结果对齐。
 */
@Getter
public final class ConfigCategoryTitleSpec {

    public enum Kind {
        TRANSLATION_KEY,
        LOCALIZED_STATIC,
        LITERAL
    }

    private final Kind kind;
    private final String translationKey;
    private final Map<String, String> localizedByLang;
    private final String literal;

    private ConfigCategoryTitleSpec(Kind kind, String translationKey, Map<String, String> localizedByLang, String literal) {
        this.kind = kind;
        this.translationKey = translationKey != null ? translationKey : "";
        this.localizedByLang = localizedByLang != null ? localizedByLang : Collections.emptyMap();
        this.literal = literal != null ? literal : "";
    }

    public static ConfigCategoryTitleSpec translationKey(String key) {
        return new ConfigCategoryTitleSpec(Kind.TRANSLATION_KEY, key, Collections.emptyMap(), "");
    }

    public static ConfigCategoryTitleSpec localized(Map<String, String> map) {
        return new ConfigCategoryTitleSpec(Kind.LOCALIZED_STATIC, "",
                Collections.unmodifiableMap(new LinkedHashMap<>(map)), "");
    }

    public static ConfigCategoryTitleSpec literal(String text) {
        return new ConfigCategoryTitleSpec(Kind.LITERAL, "", Collections.emptyMap(), text);
    }
}
