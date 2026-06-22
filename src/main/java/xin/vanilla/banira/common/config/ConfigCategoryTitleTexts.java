package xin.vanilla.banira.common.config;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;
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
            return BaniraComponent.get().literal(fb);
        }
        switch (spec.getKind()) {
            case TRANSLATION_KEY:
                if (StringUtils.isNullOrEmptyEx(spec.getTranslationKey())) {
                    return BaniraComponent.get().literal(fb);
                }
                return new ScopedComponent(modId).transClientAuto(spec.getTranslationKey());
            case LOCALIZED_STATIC:
                String picked = Translator.pickLocalizedMapValue(null, spec.getLocalizedByLang());
                if (!StringUtils.isNullOrEmptyEx(picked)) {
                    return BaniraComponent.get().literal(picked);
                }
                return BaniraComponent.get().literal(fb);
            case LITERAL:
            default:
                if (!StringUtils.isNullOrEmptyEx(spec.getLiteral())) {
                    return BaniraComponent.get().literal(spec.getLiteral());
                }
                return BaniraComponent.get().literal(fb);
        }
    }
}
