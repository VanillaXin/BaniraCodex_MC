package xin.vanilla.banira.internal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumSeason;

/**
 * 客户端专用配置（Forge CLIENT）
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link RootView} 读写 {@link ConfigHolder}（路径由代理按字段名推导）。
 */
@Config(name = "banira_codex-client", type = ModConfig.Type.CLIENT)
public class ClientConfig implements ConfigData {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 主题样式：AUTO 与界面「自动」一致时按日历季节；可固定为春夏秋冬之一以覆盖日历。",
            en_us = "GUI theme style: with screen season on Auto, uses calendar season unless you pick a fixed season here.")
    private EnumSeason guiThemeStyle = EnumSeason.AUTO;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 夜间配色：关闭则始终用日间主题；总是夜晚；指定时间段按本机时钟；自动则在游戏内按世界昼夜，主菜单等无世界时用本机 6:00–18:00 为日间。",
            en_us = "GUI night palette: Off (day only); Always night; Scheduled uses local clock; Auto uses world day/night in-game, else local 6:00–18:00 as day.")
    private EnumGuiNightMode guiNightMode = EnumGuiNightMode.OFF;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "夜间模式「指定时间段」开始时刻（从 0 点算起的分钟数，0–1439）。与结束时刻共同定义夜间区间；可跨午夜（例如 1320–360 表示 22:00–次日 6:00）。",
            en_us = "Scheduled night mode: start minute of day (0–1439). Together with end minute defines the night window; may wrap midnight (e.g. 1320–360 = 22:00–06:00).")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1439)
    private int guiNightModeStartMinute = 22 * 60;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "夜间模式「指定时间段」结束时刻（从 0 点算起的分钟数，0–1439）。",
            en_us = "Scheduled night mode: end minute of day (0–1439).")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1439)
    private int guiNightModeEndMinute = 6 * 60;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "通知日志中最多保留的条数（超出时丢弃最旧记录）。",
            en_us = "Maximum number of entries kept in the notification log (oldest dropped when exceeded).")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10000)
    private int notificationLogMaxEntries = 500;

    public ClientConfig() {
    }

    public static RootView get() {
        return ClientConfigAccess.root(ForgeConfigAdapter.getHolder(ClientConfig.class));
    }

    // region 运行时视图接口

    public interface RootView {
        EnumSeason guiThemeStyle();

        RootView guiThemeStyle(EnumSeason value);

        EnumGuiNightMode guiNightMode();

        RootView guiNightMode(EnumGuiNightMode value);

        int guiNightModeStartMinute();

        RootView guiNightModeStartMinute(int value);

        int guiNightModeEndMinute();

        RootView guiNightModeEndMinute(int value);

        int notificationLogMaxEntries();

        RootView notificationLogMaxEntries(int value);

        ConfigHolder holder();
    }

    // endregion 运行时视图接口
}
