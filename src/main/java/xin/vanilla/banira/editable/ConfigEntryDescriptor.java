package xin.vanilla.banira.editable;

import lombok.Builder;
import lombok.Getter;
import xin.vanilla.banira.editable.annotation.BaniraFieldMeta;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 配置项描述符，用于 Banira 配置编辑器渲染与网络编解码。
 */
@Getter
@Builder
public class ConfigEntryDescriptor {

    private final String path;

    private final String displayName;

    @Builder.Default
    private final List<String> tooltip = Collections.emptyList();

    @Builder.Default
    private final ConfigTooltipGuiKind tooltipGuiKind = ConfigTooltipGuiKind.MULTILINE_LITERAL;

    @Builder.Default
    private final String tooltipTranslationKey = "";

    @Builder.Default
    private final Map<String, String> tooltipLocalizedByLang = Collections.emptyMap();

    private final ConfigValueType valueType;

    private final Object defaultValue;

    private final Number minValue;

    private final Number maxValue;

    @Builder.Default
    private final int decimalPlaces = 2;

    private final Class<? extends Enum<?>> enumClass;

    @Builder.Default
    private final BaniraFieldMeta.EditPermissionPolicy editPermissionPolicy = BaniraFieldMeta.EditPermissionPolicy.INHERIT;

    private final Integer fieldEditPermissionLevel;

    private final String fieldEditVirtualPermissionKey;

    public enum ConfigValueType {
        STRING,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        ENUM,
        STRING_LIST,
        INTEGER_LIST,
        LONG_LIST,
        DOUBLE_LIST,
        BOOLEAN_LIST,
        ENUM_LIST
    }

    public static boolean isListValueType(ConfigValueType t) {
        if (t == null) {
            return false;
        }
        return switch (t) {
            case STRING_LIST, INTEGER_LIST, LONG_LIST, DOUBLE_LIST, BOOLEAN_LIST, ENUM_LIST -> true;
            default -> false;
        };
    }

    public boolean isListType() {
        return isListValueType(valueType);
    }

    public enum ConfigTooltipGuiKind {
        TRANSLATION_KEY,
        LOCALIZED_STATIC,
        MULTILINE_LITERAL
    }
}
