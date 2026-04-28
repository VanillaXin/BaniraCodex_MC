package xin.vanilla.banira.internal.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import xin.vanilla.banira.BaniraCodex;

/**
 * 通用配置
 */
@Config(name = "banira_codex-common")
public class CommonConfig implements ConfigData {

    private static final ConfigHolder<CommonConfig> HOLDER = AutoConfig.register(CommonConfig.class, Toml4jConfigSerializer::new);
    private static final CommonConfig INSTANCE = HOLDER.getConfig();
    private static final RootView ROOT_VIEW = new Root();

    public CommonConfig() {
    }

    public static RootView get() {
        return ROOT_VIEW;
    }

    public static CommonConfig instance() {
        return INSTANCE;
    }

    public static void save() {
        HOLDER.save();
    }

    @Override
    public void validatePostLoad() {
        if (help == null) help = new HelpCategory();
        if (language == null) language = new LanguageCategory();
        if (command == null) command = new CommandCategory();
        if (permission == null) permission = new PermissionCategory();
    }

    // region 运行时视图接口

    public interface RootView {
        HelpView help();

        LanguageView language();

        CommandView command();

        PermissionView permission();
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

    private HelpCategory help = new HelpCategory();
    private LanguageCategory language = new LanguageCategory();
    private CommandCategory command = new CommandCategory();
    private PermissionCategory permission = new PermissionCategory();

    public HelpCategory help() {
        return help;
    }

    public LanguageCategory language() {
        return language;
    }

    public CommandCategory command() {
        return command;
    }

    public PermissionCategory permission() {
        return permission;
    }

    private static final class Root implements RootView {
        @Override
        public HelpView help() {
            return INSTANCE.help();
        }

        @Override
        public LanguageView language() {
            return INSTANCE.language();
        }

        @Override
        public CommandView command() {
            return INSTANCE.command();
        }

        @Override
        public PermissionView permission() {
            return INSTANCE.permission();
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class HelpCategory implements HelpView {
        private String helpHeader = "-----==== Banira Codex Help (%d/%d) ====-----";

        private int helpInfoNumPerPage = 10;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class LanguageCategory implements LanguageView {
        private String defaultLanguage = "en_us";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandCategory implements CommandView {
        private String commandPrefix = "banira";

        private String commandHelp = "help";

        private String commandLanguage = "language";

        private String commandVirtualOp = "virtual_op";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionCategory implements PermissionView {
        private int virtualOpPermission = 4;

        @Deprecated
        private int editServerConfigPermission = 2;

        @Deprecated
        private String editServerConfigVirtualPermissionKey = BaniraCodex.MODID + ":" + "EDIT_SERVER_CONFIG";
    }
}
