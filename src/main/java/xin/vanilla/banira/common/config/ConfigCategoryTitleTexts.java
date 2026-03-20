package xin.vanilla.banira.common.config;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nullable;

/**
 * 将 {@link ConfigCategoryTitleSpec} 转为配置编辑器等 GUI 用的 {@link Component}。
 */
public final class ConfigCategoryTitleTexts {

    private ConfigCategoryTitleTexts() {
    }

    public static Component categoryTitleComponent(@Nullable ConfigCategoryTitleSpec spec, String modId,
                                                   @Nullable String fallbackDisplayName) {
        String fb = fallbackDisplayName != null ? fallbackDisplayName : "";
        if (spec == null) {
            return Component.literal(fb);
        }
        switch (spec.getKind()) {
            case TRANSLATION_KEY:
                if (StringUtils.isNullOrEmptyEx(spec.getTranslationKey())) {
                    return Component.literal(fb);
                }
                return Component.transClientAuto(modId, spec.getTranslationKey());
            case LOCALIZED_STATIC:
                String picked = Translator.pickLocalizedMapValue(Translator.getClientLanguage(), spec.getLocalizedByLang());
                if (!StringUtils.isNullOrEmptyEx(picked)) {
                    return Component.literal(picked);
                }
                return Component.literal(fb);
            case LITERAL:
            default:
                if (!StringUtils.isNullOrEmptyEx(spec.getLiteral())) {
                    return Component.literal(spec.getLiteral());
                }
                return Component.literal(fb);
        }
    }
}
