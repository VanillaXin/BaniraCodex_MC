package xin.vanilla.banira.internal.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumSeason;

/**
 * 客户端配置
 */
@Config(name = "banira_codex-client")
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class ClientConfig implements ConfigData {

    private static final ConfigHolder<ClientConfig> HOLDER = AutoConfig.register(ClientConfig.class, Toml4jConfigSerializer::new);
    private static final ClientConfig INSTANCE = HOLDER.getConfig();
    private static final RootView ROOT_VIEW = new Root();

    private EnumSeason guiThemeStyle = EnumSeason.AUTO;
    private EnumGuiNightMode guiNightMode = EnumGuiNightMode.OFF;
    private int guiNightModeStartMinute = 22 * 60;
    private int guiNightModeEndMinute = 6 * 60;
    private int notificationLogMaxEntries = 500;
    private int notificationMergeWindowMs = 2500;
    private int notificationBurstThreshold = 5;
    private int notificationBurstStaggerMs = 400;
    private int notificationBurstMaxExtraDelayMs = 20000;
    private boolean useCustomCursor = true;

    public static RootView get() {
        return ROOT_VIEW;
    }

    public static ClientConfig instance() {
        return INSTANCE;
    }

    public static void save() {
        HOLDER.save();
    }

    @Override
    public void validatePostLoad() {
        if (guiThemeStyle == null) guiThemeStyle = EnumSeason.AUTO;
        if (guiNightMode == null) guiNightMode = EnumGuiNightMode.OFF;
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
    }

    // endregion 运行时视图接口

    private static final class Root implements RootView {
        @Override
        public EnumSeason guiThemeStyle() {
            return INSTANCE.guiThemeStyle();
        }

        @Override
        public RootView guiThemeStyle(EnumSeason value) {
            INSTANCE.guiThemeStyle(value);
            return this;
        }

        @Override
        public EnumGuiNightMode guiNightMode() {
            return INSTANCE.guiNightMode();
        }

        @Override
        public RootView guiNightMode(EnumGuiNightMode value) {
            INSTANCE.guiNightMode(value);
            return this;
        }

        @Override
        public int guiNightModeStartMinute() {
            return INSTANCE.guiNightModeStartMinute();
        }

        @Override
        public RootView guiNightModeStartMinute(int value) {
            INSTANCE.guiNightModeStartMinute(value);
            return this;
        }

        @Override
        public int guiNightModeEndMinute() {
            return INSTANCE.guiNightModeEndMinute();
        }

        @Override
        public RootView guiNightModeEndMinute(int value) {
            INSTANCE.guiNightModeEndMinute(value);
            return this;
        }

        @Override
        public int notificationLogMaxEntries() {
            return INSTANCE.notificationLogMaxEntries();
        }

        @Override
        public RootView notificationLogMaxEntries(int value) {
            INSTANCE.notificationLogMaxEntries(value);
            return this;
        }

        @Override
        public int notificationMergeWindowMs() {
            return INSTANCE.notificationMergeWindowMs();
        }

        @Override
        public RootView notificationMergeWindowMs(int value) {
            INSTANCE.notificationMergeWindowMs(value);
            return this;
        }

        @Override
        public int notificationBurstThreshold() {
            return INSTANCE.notificationBurstThreshold();
        }

        @Override
        public RootView notificationBurstThreshold(int value) {
            INSTANCE.notificationBurstThreshold(value);
            return this;
        }

        @Override
        public int notificationBurstStaggerMs() {
            return INSTANCE.notificationBurstStaggerMs();
        }

        @Override
        public RootView notificationBurstStaggerMs(int value) {
            INSTANCE.notificationBurstStaggerMs(value);
            return this;
        }

        @Override
        public int notificationBurstMaxExtraDelayMs() {
            return INSTANCE.notificationBurstMaxExtraDelayMs();
        }

        @Override
        public RootView notificationBurstMaxExtraDelayMs(int value) {
            INSTANCE.notificationBurstMaxExtraDelayMs(value);
            return this;
        }

        @Override
        public boolean useCustomCursor() {
            return INSTANCE.useCustomCursor();
        }

        @Override
        public RootView useCustomCursor(boolean value) {
            INSTANCE.useCustomCursor(value);
            return this;
        }
    }
}
