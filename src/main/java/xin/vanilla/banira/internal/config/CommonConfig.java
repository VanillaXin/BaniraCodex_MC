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
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

/**
 * 通用（Common）配置：注解结构用于构建 ForgeConfigSpec 与配置编辑器；
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link CommonConfigView} 分层读 {@link ConfigHolder}（由配置声明生成强类型视图）。
 */
@Config(name = "banira_codex-common", type = ConfigScope.COMMON,
        generateView = true, viewUnbound = Config.UnboundAccess.DEFAULTS)
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

    public static CommonConfigView get() {
        return CommonConfigView.get();
    }


    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class HelpCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "帮助头部", en_us = "Header line for paginated help output (format string)")
        @ConfigEntry.Access(nulls = ConfigEntry.Access.NullPolicy.KEEP)
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
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandPrefix = "banira";

        @ConfigEntry.Gui.Tooltip(zh_cn = "帮助子指令名", en_us = "Subcommand name for help")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandHelp = "help";

        @ConfigEntry.Gui.Tooltip(zh_cn = "设置语言子指令名", en_us = "Subcommand name to change language")
        @ConfigEntry.Access(nulls = ConfigEntry.Access.NullPolicy.KEEP)
        private String commandLanguage = "language";

        @ConfigEntry.Gui.Tooltip(zh_cn = "虚拟OP子指令名", en_us = "Subcommand name for virtual OP")
        @ConfigEntry.Access(nulls = ConfigEntry.Access.NullPolicy.KEEP)
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

        @ConfigEntry.Gui.Tooltip(zh_cn = "修改服务端配置所需虚拟权限完整键（modId:id，\n与虚拟OP中授予的键一致）",
                en_us = "Full virtual permission key (modId:id) for editing server config; match keys granted via virtual OP")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String editServerConfigVirtualPermissionKey = BaniraCodex.MODID + ":" + "EDIT_SERVER_CONFIG";
    }
}
