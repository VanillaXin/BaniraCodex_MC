package xin.vanilla.banira.internal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumNotificationHudMode;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 客户端专用配置（Forge CLIENT）
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link ClientConfigView} 读写 {@link ConfigHolder}（由配置声明生成强类型视图）。
 */
@Config(name = "banira_codex-client", type = ConfigScope.CLIENT,
        generateView = true, viewUnbound = Config.UnboundAccess.DEFAULTS)
public class ClientConfig implements ConfigData {

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 主题样式：AUTO 与界面「自动」一致时按日历季节；\n可固定为春夏秋冬之一以覆盖日历",
            en_us = "GUI theme style: with screen season on Auto, uses calendar season unless you pick a fixed season here.")
    @ConfigEntry.Access(enumParser = "valueOfDefault")
    private EnumSeason guiThemeStyle = EnumSeason.AUTO;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "GUI 夜间配色：关闭则始终用日间主题；总是夜晚；\n指定时间段按本机时钟；自动则在游戏内按世界昼夜，\n主菜单等无世界时用本机 6:00–18:00 为日间",
            en_us = "GUI night palette: Off (day only); Always night; Scheduled uses local clock; Auto uses world day/night in-game, else local 6:00–18:00 as day.")
    @ConfigEntry.Access(enumParser = "valueOfDefault")
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

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "未读通知 HUD", en_us = "Unread notification HUD")
    private NotificationHudCategory notificationHud = new NotificationHudCategory();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "通知显示区域", en_us = "Notification regions")
    private NotificationRegionsCategory notificationRegions = new NotificationRegionsCategory();

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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.Tooltip(zh_cn = "控制已适配模组的背包界面按钮由谁统一显示\n选择 FTB Library 但未安装时自动改由 Banira 显示",
            en_us = "Choose who displays supported third-party inventory buttons\nFalls back to Banira when FTB Library is selected but unavailable")
    private EnumExternalInventoryButtonHost externalInventoryButtonHost =
            EnumExternalInventoryButtonHost.BANIRA;

    public ClientConfig() {
    }

    public static ClientConfigView get() {
        return ClientConfigView.get();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class NotificationHudCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "显示模式", en_us = "Display mode")
        private EnumNotificationHudMode mode = EnumNotificationHudMode.TOGGLE;

        @ConfigEntry.Gui.KeyChords
        @ConfigEntry.Gui.Tooltip(zh_cn = "显示按键\n点击录入组合键，不拦截原版按键行为", en_us = "Display shortcuts\nCapture a key combination; vanilla key actions remain available")
        private List<String> keys = new ArrayList<>(Arrays.asList("Tab"));

        @ConfigEntry.BoundedDouble(min = 0, max = 1, decimalPlaces = 3)
        @ConfigEntry.Gui.Tooltip(zh_cn = "水平位置\n0为最左侧，1为最右侧", en_us = "Horizontal position\n0 is left; 1 is right")
        private double x = 0;

        @ConfigEntry.BoundedDouble(min = 0, max = 1, decimalPlaces = 3)
        @ConfigEntry.Gui.Tooltip(zh_cn = "垂直位置\n0为顶部，1为底部", en_us = "Vertical position\n0 is top; 1 is bottom")
        private double y = .5;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class NotificationRegionCategory {
        @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
        @ConfigEntry.Gui.Tooltip(zh_cn = "区域宽度，占屏幕宽度的百分比", en_us = "Region width as a percentage of the screen")
        private int widthPercent = 30;

        @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
        @ConfigEntry.Gui.Tooltip(zh_cn = "区域高度，占屏幕高度的百分比", en_us = "Region height as a percentage of the screen")
        private int heightPercent = 32;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        @ConfigEntry.Gui.Tooltip(zh_cn = "此区域同时显示的气泡上限", en_us = "Maximum visible bubbles in this region")
        private int visibleLimit = 3;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class NotificationRegionsCategory {
        @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
        @ConfigEntry.Gui.Tooltip(zh_cn = "全部区域同时显示的气泡上限", en_us = "Maximum visible bubbles across all regions")
        private int visibleLimit = 6;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 500)
        @ConfigEntry.Gui.Tooltip(zh_cn = "浮层队列上限，包含正在显示的气泡\n满载时新消息只记入历史和未读数量", en_us = "Overlay queue capacity, including visible bubbles\nWhen full, new messages still enter history and unread counts")
        private int queueLimit = 64;

        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
        @ConfigEntry.Gui.Tooltip(zh_cn = "区域与屏幕边缘的间距", en_us = "Margin between notification regions and screen edges")
        private int margin = 6;

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "左上", en_us = "Top left")
        private NotificationRegionCategory topLeft = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "顶部居中", en_us = "Top center")
        private NotificationRegionCategory topCenter = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "右上", en_us = "Top right")
        private NotificationRegionCategory topRight = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "左侧居中", en_us = "Left center")
        private NotificationRegionCategory leftCenter = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "中心", en_us = "Center")
        private NotificationRegionCategory center = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "右侧居中", en_us = "Right center")
        private NotificationRegionCategory rightCenter = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "左下", en_us = "Bottom left")
        private NotificationRegionCategory bottomLeft = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "底部居中", en_us = "Bottom center")
        private NotificationRegionCategory bottomCenter = new NotificationRegionCategory();

        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "右下", en_us = "Bottom right")
        private NotificationRegionCategory bottomRight = new NotificationRegionCategory();
    }

}
