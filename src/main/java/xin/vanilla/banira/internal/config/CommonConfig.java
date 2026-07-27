package xin.vanilla.banira.internal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.BaniraCommonSettings;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

/**
 * 通用（Common）配置：注解结构用于构建 ModConfigSpec 与配置编辑器；
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link RootView} 分层读 {@link ConfigHolder}（路径由代理按字段名推导，无需 Key 与手写 get/set）。
 */
@Config(name = "banira_codex-common", type = ConfigScope.COMMON)
public class CommonConfig implements ConfigData {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "帮助相关设置", en_us = "Help-related settings")
    private HelpCategory help = new HelpCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "语言相关设置", en_us = "Language settings")
    private LanguageCategory language = new LanguageCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "指令名称设置", en_us = "Command name settings (prefix and subcommands)")
    private CommandCategory command = new CommandCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "权限相关设置", en_us = "Permission settings")
    private PermissionCategory permission = new PermissionCategory();

    public CommonConfig() {
    }

    public static RootView get() {
        return CommonConfigAccess.root(ForgeConfigAdapter.getHolder(CommonConfig.class));
    }

    // region 运行时视图接口

    public interface RootView {
        HelpView help();

        LanguageView language();

        CommandView command();

        PermissionView permission();

        ConfigHolder holder();
    }

    public interface HelpView {
        String helpHeader();

        HelpView helpHeader(String value);

        int helpInfoNumPerPage();

        HelpView helpInfoNumPerPage(int value);
    }

    public interface LanguageView {
        String defaultLanguage();

        LanguageView defaultLanguage(String value);
    }

    public interface CommandView {
        String commandPrefix();

        CommandView commandPrefix(String value);

        String commandHelp();

        CommandView commandHelp(String value);

        String commandLanguage();

        CommandView commandLanguage(String value);

        String commandVirtualOp();

        CommandView commandVirtualOp(String value);
    }

    public interface PermissionView {
        int virtualOpPermission();

        PermissionView virtualOpPermission(int value);

        int editServerConfigPermission();

        PermissionView editServerConfigPermission(int value);

        String editServerConfigVirtualPermissionKey();

        PermissionView editServerConfigVirtualPermissionKey(String value);
    }

    // endregion 运行时视图接口

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class HelpCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "帮助头部", en_us = "Header line for paginated help output (format string)")
        private String helpHeader = BaniraCommonSettings.DEFAULT_HELP_HEADER;

        @ConfigEntry.Gui.Tooltip(zh_cn = "每页帮助数量", en_us = "Number of help lines per page")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        private int helpInfoNumPerPage = 10;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class LanguageCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "默认语言", en_us = "Default language code (e.g. en_us, zh_cn)")
        private String defaultLanguage = "en_us";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "指令前缀", en_us = "Root command prefix (namespace)")
        private String commandPrefix = "banira";

        @ConfigEntry.Gui.Tooltip(zh_cn = "帮助子指令名", en_us = "Subcommand name for help")
        private String commandHelp = "help";

        @ConfigEntry.Gui.Tooltip(zh_cn = "设置语言子指令名", en_us = "Subcommand name to change language")
        private String commandLanguage = "language";

        @ConfigEntry.Gui.Tooltip(zh_cn = "虚拟OP子指令名", en_us = "Subcommand name for virtual OP")
        private String commandVirtualOp = "virtual_op";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "虚拟OP所需权限等级", en_us = "Permission level (0–4) required to use virtual OP")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int virtualOpPermission = 4;

        @ConfigEntry.Gui.Tooltip(zh_cn = "修改服务端配置所需权限等级（配置编辑器同步/拉取）",
                en_us = "Permission level (0–4) to edit server config (config editor sync / pull)")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int editServerConfigPermission = 2;

        @ConfigEntry.Gui.Tooltip(zh_cn = "修改服务端配置所需虚拟权限完整键（modId:id，与虚拟OP中授予的键一致）",
                en_us = "Full virtual permission key (modId:id) for editing server config; match keys granted via virtual OP")
        private String editServerConfigVirtualPermissionKey = BaniraCodex.MODID + ":" + "EDIT_SERVER_CONFIG";
    }
}
