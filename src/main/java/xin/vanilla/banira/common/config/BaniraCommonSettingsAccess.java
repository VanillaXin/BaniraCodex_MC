package xin.vanilla.banira.common.config;

import xin.vanilla.banira.api.BaniraConfigs;

import xin.vanilla.banira.api.BaniraCommonSettings;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.config.CommonConfigView;

import javax.annotation.Nonnull;

/**
 * 连接公共设置门面与 Banira 自身配置实现。
 */
public final class BaniraCommonSettingsAccess {
    private static final CommonConfig.HelpCategory DEFAULT_HELP = new CommonConfig.HelpCategory();
    private static final CommonConfig.LanguageCategory DEFAULT_LANGUAGE = new CommonConfig.LanguageCategory();

    private BaniraCommonSettingsAccess() {
    }

    public static int helpInfoNumPerPage() {
        CommonConfigView config = configOrNull();
        int value = config != null
                ? config.help().helpInfoNumPerPage()
                : DEFAULT_HELP.helpInfoNumPerPage();
        return Math.max(1, value);
    }

    @Nonnull
    public static String helpHeader() {
        CommonConfigView config = configOrNull();
        String value = config != null ? config.help().helpHeader() : DEFAULT_HELP.helpHeader();
        return value == null || value.isEmpty() ? BaniraCommonSettings.DEFAULT_HELP_HEADER : value;
    }

    @Nonnull
    public static String defaultLanguage() {
        CommonConfigView config = configOrNull();
        return config != null
                ? config.language().defaultLanguage()
                : DEFAULT_LANGUAGE.defaultLanguage();
    }

    public static void defaultLanguage(@Nonnull String language) {
        CommonConfigView config = CommonConfig.get();
        config.language().defaultLanguage(language);
        ConfigHolder holder = BaniraConfigs.holder(CommonConfig.class);
        if (holder != null) {
            holder.save();
        }
    }

    private static CommonConfigView configOrNull() {
        try {
            return CommonConfig.get();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }
}
