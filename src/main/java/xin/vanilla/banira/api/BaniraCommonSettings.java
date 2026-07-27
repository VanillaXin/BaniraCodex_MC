package xin.vanilla.banira.api;

import xin.vanilla.banira.common.config.BaniraCommonSettingsAccess;

import javax.annotation.Nonnull;
import java.util.IllegalFormatException;

/**
 * 向子模组公开 Banira 的通用帮助与服务端语言设置。
 */
public final class BaniraCommonSettings {
    public static final String DEFAULT_HELP_HEADER = "-----==== %s Help (%d/%d) ====-----";

    private BaniraCommonSettings() {
    }

    public static int helpInfoNumPerPage() {
        return BaniraCommonSettingsAccess.helpInfoNumPerPage();
    }

    @Nonnull
    public static String defaultLanguage() {
        return BaniraCommonSettingsAccess.defaultLanguage();
    }

    public static void defaultLanguage(@Nonnull String language) {
        BaniraCommonSettingsAccess.defaultLanguage(language);
    }

    @Nonnull
    public static String formatHelpHeader(@Nonnull String moduleName, int page, int pages) {
        String template = BaniraCommonSettingsAccess.helpHeader();
        try {
            return String.format(template, moduleName, page, pages);
        } catch (IllegalFormatException ignored) {
            return String.format(DEFAULT_HELP_HEADER, moduleName, page, pages);
        }
    }
}
