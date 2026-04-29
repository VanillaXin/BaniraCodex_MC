package xin.vanilla.banira.editable;

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
        return switch (spec.getKind()) {
            case TRANSLATION_KEY -> {
                String k = spec.getTranslationKey();
                if (StringUtils.isNullOrEmptyEx(k)) {
                    yield BaniraComponent.get().literal(fb);
                }
                if (k.startsWith("text.autoconfig.")) {
                    yield BaniraComponent.get().transClient(k);
                }
                yield new ScopedComponent(modId).transClientAuto(k);
            }
            case LOCALIZED_STATIC -> {
                String picked = Translator.pickLocalizedMapValue(Translator.getClientLanguage(), spec.getLocalizedByLang());
                if (!StringUtils.isNullOrEmptyEx(picked)) {
                    yield BaniraComponent.get().literal(picked);
                }
                yield BaniraComponent.get().literal(fb);
            }
            case LITERAL -> {
                if (!StringUtils.isNullOrEmptyEx(spec.getLiteral())) {
                    yield BaniraComponent.get().literal(spec.getLiteral());
                }
                yield BaniraComponent.get().literal(fb);
            }
        };
    }
}
