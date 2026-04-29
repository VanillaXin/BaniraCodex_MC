package xin.vanilla.banira.editable;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
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
        return switch (desc.getTooltipGuiKind()) {
            case TRANSLATION_KEY -> !StringUtils.isNullOrEmptyEx(desc.getTooltipTranslationKey());
            case LOCALIZED_STATIC -> {
                Map<String, String> loc = desc.getTooltipLocalizedByLang();
                if (loc == null || loc.isEmpty()) {
                    yield false;
                }
                for (String v : loc.values()) {
                    if (!StringUtils.isNullOrEmptyEx(v)) {
                        yield true;
                    }
                }
                yield false;
            }
            case MULTILINE_LITERAL -> {
                List<String> lines = desc.getTooltip();
                yield lines != null && !lines.isEmpty();
            }
        };
    }

    public static Component guiTooltipComponent(ConfigEntryDescriptor desc, String modId) {
        if (desc == null) {
            return BaniraComponent.get().literal("");
        }
        return switch (desc.getTooltipGuiKind()) {
            case TRANSLATION_KEY -> {
                String k = desc.getTooltipTranslationKey();
                if (k != null && k.startsWith("text.autoconfig.")) {
                    yield BaniraComponent.get().transClient(k);
                }
                yield new ScopedComponent(modId).transClientAuto(k);
            }
            case LOCALIZED_STATIC ->
                    BaniraComponent.get().literal(Translator.pickLocalizedMapValue(Translator.getClientLanguage(),
                            desc.getTooltipLocalizedByLang()));
            case MULTILINE_LITERAL -> {
                List<String> lines = desc.getTooltip();
                if (lines == null || lines.isEmpty()) {
                    yield BaniraComponent.get().literal("");
                }
                yield BaniraComponent.get().literal(String.join("\n", lines));
            }
        };
    }
}
