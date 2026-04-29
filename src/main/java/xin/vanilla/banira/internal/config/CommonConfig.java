package xin.vanilla.banira.internal.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.editable.EditableConfigRegistry;

/**
 * 通用配置
 */
@Config(name = "banira_codex-common")
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class CommonConfig implements ConfigData {

    private static final ConfigHolder<CommonConfig> HOLDER = AutoConfig.register(CommonConfig.class, Toml4jConfigSerializer::new);

    static {
        EditableConfigRegistry.registerAutoConfig(BaniraCodex.MODID, HOLDER, true);
    }

    @ConfigEntry.Gui.CollapsibleObject
    private HelpCategory help = new HelpCategory();

    @ConfigEntry.Gui.CollapsibleObject
    private LanguageCategory language = new LanguageCategory();

    @ConfigEntry.Gui.CollapsibleObject
    private CommandCategory command = new CommandCategory();

    @ConfigEntry.Gui.CollapsibleObject
    private PermissionCategory permission = new PermissionCategory();

    public CommonConfig() {
    }

    public static CommonConfig get() {
        return HOLDER.getConfig();
    }

    public static void save() {
        HOLDER.save();
    }

    @Override
    public void validatePostLoad() {
        if (help == null) {
            help = new HelpCategory();
        }
        if (language == null) {
            language = new LanguageCategory();
        }
        if (command == null) {
            command = new CommandCategory();
        }
        if (permission == null) {
            permission = new PermissionCategory();
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class HelpCategory {
        private String helpHeader = "-----==== Banira Codex Help (%d/%d) ====-----";

        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        private int helpInfoNumPerPage = 10;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class LanguageCategory {
        private String defaultLanguage = "en_us";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandCategory {
        private String commandPrefix = "banira";

        private String commandHelp = "help";

        private String commandLanguage = "language";

        private String commandVirtualOp = "virtual_op";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionCategory {
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int virtualOpPermission = 4;

        @ConfigEntry.BoundedDiscrete(max = 4)
        private int editServerConfigPermission = 2;

        private String editServerConfigVirtualPermissionKey = BaniraCodex.MODID + ":" + "EDIT_SERVER_CONFIG";
    }
}
