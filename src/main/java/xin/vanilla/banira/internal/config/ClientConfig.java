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
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.editable.EditableConfigRegistry;

/**
 * 客户端配置
 */
@Config(name = "banira_codex-client")
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class ClientConfig implements ConfigData {

    private static final ConfigHolder<ClientConfig> HOLDER = AutoConfig.register(ClientConfig.class, Toml4jConfigSerializer::new);

    static {
        EditableConfigRegistry.registerAutoConfig(BaniraCodex.MODID, HOLDER, false);
    }

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
    private EnumSeason guiThemeStyle = EnumSeason.AUTO;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
    private EnumGuiNightMode guiNightMode = EnumGuiNightMode.OFF;

    @ConfigEntry.BoundedDiscrete(max = 1439)
    private int guiNightModeStartMinute = 22 * 60;

    @ConfigEntry.BoundedDiscrete(max = 1439)
    private int guiNightModeEndMinute = 6 * 60;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 10000)
    private int notificationLogMaxEntries = 500;

    @ConfigEntry.BoundedDiscrete(max = 60000)
    private int notificationMergeWindowMs = 2500;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    private int notificationBurstThreshold = 5;

    @ConfigEntry.BoundedDiscrete(max = 10000)
    private int notificationBurstStaggerMs = 400;

    @ConfigEntry.BoundedDiscrete(max = 120000)
    private int notificationBurstMaxExtraDelayMs = 20000;

    private boolean useCustomCursor = true;

    public static ClientConfig get() {
        return HOLDER.getConfig();
    }

    public static void save() {
        HOLDER.save();
    }

    @Override
    public void validatePostLoad() {
        if (guiThemeStyle == null) {
            guiThemeStyle = EnumSeason.AUTO;
        }
        if (guiNightMode == null) {
            guiNightMode = EnumGuiNightMode.OFF;
        }
    }
}
