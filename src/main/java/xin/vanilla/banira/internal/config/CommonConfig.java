package xin.vanilla.banira.internal.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

/**
 * 服务器配置 - 层级 API，用于构建 ForgeConfigSpec 与运行时访问。
 * <p>
 * 使用方式：CommonConfig.get().command().commandPrefix()
 */
@Getter
@Setter
@Accessors(fluent = true)
@Config(name = "banira_codex-common")
public class CommonConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "帮助相关设置")
    private HelpCategory help = new HelpCategory();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "语言相关设置")
    private LanguageCategory language = new LanguageCategory();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "指令名称设置")
    private CommandCategory command = new CommandCategory();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(value = "权限相关设置")
    private PermissionCategory permission = new PermissionCategory();

    private final ConfigHolder holder;
    private final Help helpApi;
    private final Language languageApi;
    private final Command commandApi;
    private final Permission permissionApi;

    CommonConfig(ConfigHolder holder) {
        this.holder = holder;
        this.helpApi = new Help(holder);
        this.languageApi = new Language(holder);
        this.commandApi = new Command(holder);
        this.permissionApi = new Permission(holder);
    }

    /**
     * 获取配置实例
     */
    public static CommonConfig get() {
        return new CommonConfig(ForgeConfigAdapter.getHolder(CommonConfig.class));
    }

    /**
     * 层级 API：帮助
     */
    public Help help() {
        return helpApi;
    }

    /**
     * 层级 API：语言
     */
    public Language language() {
        return languageApi;
    }

    /**
     * 层级 API：指令
     */
    public Command command() {
        return commandApi;
    }

    /**
     * 层级 API：权限
     */
    public Permission permission() {
        return permissionApi;
    }

    public ConfigHolder holder() {
        return holder;
    }

    // region Spec 构建用嵌套类

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class HelpCategory {
        @ConfigEntry.Gui.Tooltip(value = "帮助头部")
        private String helpHeader = "-----==== Banira Codex Help (%d/%d) ====-----";

        @ConfigEntry.Gui.Tooltip(value = "每页帮助数量")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 9999)
        private int helpInfoNumPerPage = 10;
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class LanguageCategory {
        @ConfigEntry.Gui.Tooltip(value = "默认语言")
        private String defaultLanguage = "en_us";
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class CommandCategory {
        @ConfigEntry.Gui.Tooltip(value = "指令前缀")
        private String commandPrefix = "banira";

        @ConfigEntry.Gui.Tooltip(value = "设置语言子指令名")
        private String commandLanguage = "language";

        @ConfigEntry.Gui.Tooltip(value = "虚拟OP子指令名")
        private String commandVirtualOp = "virtual_op";
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class PermissionCategory {
        @ConfigEntry.Gui.Tooltip(value = "虚拟OP所需权限等级")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 4)
        private int virtualOpPermission = 4;
    }

    // endregion

    // region 层级 API 访问器

    public static final class Help {
        private final ConfigHolder holder;

        Help(ConfigHolder holder) {
            this.holder = holder;
        }

        public String helpHeader() {
            return holder.get("help.helpHeader");
        }

        public Help helpHeader(String value) {
            holder.set("help.helpHeader", value);
            return this;
        }

        public int helpInfoNumPerPage() {
            Integer v = holder.get("help.helpInfoNumPerPage");
            return v != null ? v : 10;
        }

        public Help helpInfoNumPerPage(int value) {
            holder.set("help.helpInfoNumPerPage", value);
            return this;
        }
    }

    public static final class Language {
        private final ConfigHolder holder;

        Language(ConfigHolder holder) {
            this.holder = holder;
        }

        public String defaultLanguage() {
            return holder.get("language.defaultLanguage");
        }

        public Language defaultLanguage(String value) {
            holder.set("language.defaultLanguage", value);
            return this;
        }
    }

    public static final class Command {
        private final ConfigHolder holder;

        Command(ConfigHolder holder) {
            this.holder = holder;
        }

        public String commandPrefix() {
            String v = holder.get("command.commandPrefix");
            return v != null && !v.isEmpty() ? v : "banira";
        }

        public Command commandPrefix(String value) {
            holder.set("command.commandPrefix", value);
            return this;
        }

        public String commandLanguage() {
            return holder.get("command.commandLanguage");
        }

        public Command commandLanguage(String value) {
            holder.set("command.commandLanguage", value);
            return this;
        }

        public String commandVirtualOp() {
            return holder.get("command.commandVirtualOp");
        }

        public Command commandVirtualOp(String value) {
            holder.set("command.commandVirtualOp", value);
            return this;
        }
    }

    public static final class Permission {
        private final ConfigHolder holder;

        Permission(ConfigHolder holder) {
            this.holder = holder;
        }

        public int virtualOpPermission() {
            Integer v = holder.get("permission.virtualOpPermission");
            return v != null ? v : 4;
        }

        public Permission virtualOpPermission(int value) {
            holder.set("permission.virtualOpPermission", value);
            return this;
        }
    }

    // endregion
}
