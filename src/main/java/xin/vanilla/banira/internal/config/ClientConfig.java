package xin.vanilla.banira.internal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumSeason;

/**
 * 客户端专用配置（Forge CLIENT）
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link RootView} 读写 {@link ConfigHolder}（路径由代理按字段名推导）。
 */
@Config(name = "banira_codex-client", type = ConfigScope.CLIENT)
public class ClientConfig implements ConfigData {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 主题样式：AUTO 与界面「自动」一致时按日历季节；\n可固定为春夏秋冬之一以覆盖日历",
            en_us = "GUI theme style: with screen season on Auto, uses calendar season unless you pick a fixed season here.")
    private EnumSeason guiThemeStyle = EnumSeason.AUTO;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 夜间配色：关闭则始终用日间主题；总是夜晚；\n指定时间段按本机时钟；自动则在游戏内按世界昼夜，\n主菜单等无世界时用本机 6:00–18:00 为日间",
            en_us = "GUI night palette: Off (day only); Always night; Scheduled uses local clock; Auto uses world day/night in-game, else local 6:00–18:00 as day.")
    private EnumGuiNightMode guiNightMode = EnumGuiNightMode.OFF;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "夜间模式「指定时间段」开始时刻（从 0 点算起的分钟数，0–1439）\n与结束时刻共同定义夜间区间\n可跨午夜（例如 1320–360 表示 22:00–次日 6:00）",
            en_us = "Scheduled night mode: start minute of day (0–1439). Together with end minute defines the night window; may wrap midnight (e.g. 1320–360 = 22:00–06:00).")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1439)
    private int guiNightModeStartMinute = 22 * 60;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "夜间模式「指定时间段」结束时刻（从 0 点算起的分钟数，0–1439）",
            en_us = "Scheduled night mode: end minute of day (0–1439).")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1439)
    private int guiNightModeEndMinute = 6 * 60;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "通知日志中最多保留的条数（超出时丢弃最旧记录）",
            en_us = "Maximum number of entries kept in the notification log (oldest dropped when exceeded).")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10000)
    private int notificationLogMaxEntries = 500;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "浮层通知：相同类型且内容一致时，\n在此时间窗（毫秒）内到达的重复项合并为一条并显示次数\n0 关闭合并",
            en_us = "HUD notifications: duplicate same type + content within this window (ms) merge into one with a count; 0 disables.")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 60000)
    private int notificationMergeWindowMs = 2500;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "浮层通知：屏幕上未结束的通知达到此数量后，\n新通知按条递增延后显示（毫秒间隔见下一项）；至少为 1",
            en_us = "HUD notifications: when this many are still active, newer ones are staggered (see next option). Minimum 1.")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
    private int notificationBurstThreshold = 5;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "浮层通知：超过阈值后，\n每条多出的通知在「上一条」基础上再延后显示\n0 关闭延后",
            en_us = "HUD notifications: extra delay per notification beyond the burst threshold; 0 disables staggering.")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 10000)
    private int notificationBurstStaggerMs = 400;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "浮层通知：单条通知因突发队列产生的最大额外延后（毫秒），避免过久不显示；\n0 表示不限制",
            en_us = "HUD notifications: cap on extra delay from burst queue; 0 means no cap.")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 120000)
    private int notificationBurstMaxExtraDelayMs = 20000;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "在 Banira GUI 中使用本 Mod 绘制的自定义鼠标指针；\n关闭则使用系统默认光标",
            en_us = "Use this mod's drawn cursor in Banira GUIs; when off, the system default cursor is shown.")
    private boolean useCustomCursor = true;

    public ClientConfig() {
    }

    public static RootView get() {
        return ClientConfigAccess.root(BaniraConfigHandles.holder(ClientConfig.class));
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

        int notificationMergeWindowMs();

        RootView notificationMergeWindowMs(int value);

        int notificationBurstThreshold();

        RootView notificationBurstThreshold(int value);

        int notificationBurstStaggerMs();

        RootView notificationBurstStaggerMs(int value);

        int notificationBurstMaxExtraDelayMs();

        RootView notificationBurstMaxExtraDelayMs(int value);

        boolean useCustomCursor();

        RootView useCustomCursor(boolean value);

        ConfigHolder holder();
    }

    // endregion 运行时视图接口
}
