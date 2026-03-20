package xin.vanilla.banira.internal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

/**
 * 通用（Common）配置：注解结构用于构建 ForgeConfigSpec 与配置编辑器；
 * <p>
 * 运行时通过 {@code get().help()} / {@code command()} 等分层 API 读 {@link ConfigHolder}。
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@Config(name = "banira_codex-common", type = ModConfig.Type.COMMON)
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

    private final ConfigHolder holder;
    private final Help helpApi;
    private final Language languageApi;
    private final Command commandApi;
    private final Permission permissionApi;

    private CommonConfig() {
        this(null);
    }

    CommonConfig(ConfigHolder holder) {
        this.holder = holder;
        this.helpApi = new Help(holder);
        this.languageApi = new Language(holder);
        this.commandApi = new Command(holder);
        this.permissionApi = new Permission(holder);
    }

    public static CommonConfig get() {
        return new CommonConfig(ForgeConfigAdapter.getHolder(CommonConfig.class));
    }

    public Help help() {
        return helpApi;
    }

    public Language language() {
        return languageApi;
    }

    public Command command() {
        return commandApi;
    }

    public Permission permission() {
        return permissionApi;
    }

    /**
     * {@link ConfigHolder#get(String)} / {@link ConfigHolder#set(String, Object)} 使用的路径常量。
     */
    public static final class Key {

        private Key() {
        }

        public static final String HELP_HEADER = "help.helpHeader";
        public static final String HELP_INFO_NUM_PER_PAGE = "help.helpInfoNumPerPage";
        public static final String LANGUAGE_DEFAULT = "language.defaultLanguage";
        public static final String COMMAND_PREFIX = "command.commandPrefix";
        public static final String COMMAND_HELP = "command.commandHelp";
        public static final String COMMAND_LANGUAGE = "command.commandLanguage";
        public static final String COMMAND_VIRTUAL_OP = "command.commandVirtualOp";
        public static final String PERMISSION_VIRTUAL_OP = "permission.virtualOpPermission";
        public static final String PERMISSION_EDIT_SERVER_CONFIG = "permission.editServerConfigPermission";
        public static final String PERMISSION_EDIT_SERVER_CONFIG_VKEY = "permission.editServerConfigVirtualPermissionKey";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class HelpCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "帮助头部", en_us = "Header line for paginated help output (format string)")
        private String helpHeader = "-----==== Banira Codex Help (%d/%d) ====-----";

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

    public static final class Help {
        private final ConfigHolder holder;

        Help(ConfigHolder holder) {
            this.holder = holder;
        }

        public String helpHeader() {
            if (holder == null) {
                return null;
            }
            return holder.get(Key.HELP_HEADER);
        }

        public Help helpHeader(String value) {
            if (holder != null) {
                holder.set(Key.HELP_HEADER, value);
            }
            return this;
        }

        public int helpInfoNumPerPage() {
            if (holder == null) {
                return 10;
            }
            Integer v = holder.get(Key.HELP_INFO_NUM_PER_PAGE);
            return v != null ? v : 10;
        }

        public Help helpInfoNumPerPage(int value) {
            if (holder != null) {
                holder.set(Key.HELP_INFO_NUM_PER_PAGE, value);
            }
            return this;
        }
    }

    public static final class Language {
        private final ConfigHolder holder;

        Language(ConfigHolder holder) {
            this.holder = holder;
        }

        public String defaultLanguage() {
            if (holder == null) {
                return "en_us";
            }
            String v = holder.get(Key.LANGUAGE_DEFAULT);
            return v != null ? v : "en_us";
        }

        public Language defaultLanguage(String value) {
            if (holder != null) {
                holder.set(Key.LANGUAGE_DEFAULT, value);
            }
            return this;
        }
    }

    public static final class Command {
        private final ConfigHolder holder;

        Command(ConfigHolder holder) {
            this.holder = holder;
        }

        public String commandPrefix() {
            if (holder == null) {
                return "banira";
            }
            String v = holder.get(Key.COMMAND_PREFIX);
            return v != null && !v.isEmpty() ? v : "banira";
        }

        public Command commandPrefix(String value) {
            if (holder != null) {
                holder.set(Key.COMMAND_PREFIX, value);
            }
            return this;
        }

        public String commandHelp() {
            if (holder == null) {
                return "help";
            }
            String v = holder.get(Key.COMMAND_HELP);
            return v != null && !v.isEmpty() ? v : "help";
        }

        public Command commandHelp(String value) {
            if (holder != null) {
                holder.set(Key.COMMAND_HELP, value);
            }
            return this;
        }

        public String commandLanguage() {
            if (holder == null) {
                return null;
            }
            return holder.get(Key.COMMAND_LANGUAGE);
        }

        public Command commandLanguage(String value) {
            if (holder != null) {
                holder.set(Key.COMMAND_LANGUAGE, value);
            }
            return this;
        }

        public String commandVirtualOp() {
            if (holder == null) {
                return null;
            }
            return holder.get(Key.COMMAND_VIRTUAL_OP);
        }

        public Command commandVirtualOp(String value) {
            if (holder != null) {
                holder.set(Key.COMMAND_VIRTUAL_OP, value);
            }
            return this;
        }
    }

    public static final class Permission {
        private final ConfigHolder holder;

        Permission(ConfigHolder holder) {
            this.holder = holder;
        }

        public int virtualOpPermission() {
            if (holder == null) {
                return 4;
            }
            Integer v = holder.get(Key.PERMISSION_VIRTUAL_OP);
            return v != null ? v : 4;
        }

        public Permission virtualOpPermission(int value) {
            if (holder != null) {
                holder.set(Key.PERMISSION_VIRTUAL_OP, value);
            }
            return this;
        }

        public int editServerConfigPermission() {
            if (holder == null) {
                return 2;
            }
            Integer v = holder.get(Key.PERMISSION_EDIT_SERVER_CONFIG);
            return v != null ? v : 2;
        }

        public Permission editServerConfigPermission(int value) {
            if (holder != null) {
                holder.set(Key.PERMISSION_EDIT_SERVER_CONFIG, value);
            }
            return this;
        }

        public String editServerConfigVirtualPermissionKey() {
            if (holder == null) {
                return BaniraCodex.MODID + ":" + "EDIT_SERVER_CONFIG";
            }
            String v = holder.get(Key.PERMISSION_EDIT_SERVER_CONFIG_VKEY);
            return v != null && !v.isEmpty() ? v : BaniraCodex.MODID + ":" + "EDIT_SERVER_CONFIG";
        }

        public Permission editServerConfigVirtualPermissionKey(String value) {
            if (holder != null) {
                holder.set(Key.PERMISSION_EDIT_SERVER_CONFIG_VKEY, value);
            }
            return this;
        }
    }
}
