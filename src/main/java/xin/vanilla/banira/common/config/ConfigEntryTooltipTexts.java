package xin.vanilla.banira.common.config;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.Translator;

import java.util.List;
import java.util.Map;

/**
 * 将 {@link ConfigEntryDescriptor} 中的 Tooltip 元数据转为配置编辑器用的 {@link Component}。
 */
public final class ConfigEntryTooltipTexts {

    private ConfigEntryTooltipTexts() {
    }

    public static boolean hasGuiTooltip(ConfigEntryDescriptor desc) {
        if (desc == null) {
            return false;
        }
        switch (desc.getTooltipGuiKind()) {
            case TRANSLATION_KEY:
                return !StringUtils.isNullOrEmptyEx(desc.getTooltipTranslationKey());
            case LOCALIZED_STATIC:
                Map<String, String> loc = desc.getTooltipLocalizedByLang();
                if (loc == null || loc.isEmpty()) {
                    return false;
                }
                for (String v : loc.values()) {
                    if (!StringUtils.isNullOrEmptyEx(v)) {
                        return true;
                    }
                }
                return false;
            case MULTILINE_LITERAL:
            default:
                List<String> lines = desc.getTooltip();
                return lines != null && !lines.isEmpty();
        }
    }

    public static Component guiTooltipComponent(ConfigEntryDescriptor desc, String modId) {
        if (desc == null) {
            return Component.literal("");
        }
        switch (desc.getTooltipGuiKind()) {
            case TRANSLATION_KEY:
                return Component.transClientAuto(modId, desc.getTooltipTranslationKey());
            case LOCALIZED_STATIC:
                return Component.literal(Translator.pickLocalizedMapValue(Translator.getClientLanguage(),
                        desc.getTooltipLocalizedByLang()));
            case MULTILINE_LITERAL:
            default:
                List<String> lines = desc.getTooltip();
                if (lines == null || lines.isEmpty()) {
                    return Component.literal("");
                }
                return Component.literal(String.join("\n", lines));
        }
    }
}
