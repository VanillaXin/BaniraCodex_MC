package xin.vanilla.banira.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.common.enums.EnumGuiNightMode;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;

public final class BaniraConfigScreenFactory {
    private BaniraConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        ClientConfig client = ClientConfig.instance();
        CommonConfig common = CommonConfig.instance();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.banira_codex.config"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory clientCategory = builder.getOrCreateCategory(Component.translatable("word.banira_codex.client_config"));
        clientCategory.addEntry(entries.startEnumSelector(Component.translatable("word.banira_codex.gui_theme_style"), EnumSeason.class, client.guiThemeStyle())
                .setDefaultValue(EnumSeason.AUTO)
                .setSaveConsumer(client::guiThemeStyle)
                .build());
        clientCategory.addEntry(entries.startEnumSelector(Component.translatable("word.banira_codex.gui_night_mode"), EnumGuiNightMode.class, client.guiNightMode())
                .setDefaultValue(EnumGuiNightMode.OFF)
                .setSaveConsumer(client::guiNightMode)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.gui_night_mode_start_minute"), client.guiNightModeStartMinute())
                .setDefaultValue(22 * 60)
                .setMin(0)
                .setMax(1439)
                .setSaveConsumer(client::guiNightModeStartMinute)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.gui_night_mode_end_minute"), client.guiNightModeEndMinute())
                .setDefaultValue(6 * 60)
                .setMin(0)
                .setMax(1439)
                .setSaveConsumer(client::guiNightModeEndMinute)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.notification_log_max_entries"), client.notificationLogMaxEntries())
                .setDefaultValue(500)
                .setMin(1)
                .setMax(10000)
                .setSaveConsumer(client::notificationLogMaxEntries)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.notification_merge_window_ms"), client.notificationMergeWindowMs())
                .setDefaultValue(2500)
                .setMin(0)
                .setMax(60000)
                .setSaveConsumer(client::notificationMergeWindowMs)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.notification_burst_threshold"), client.notificationBurstThreshold())
                .setDefaultValue(5)
                .setMin(1)
                .setMax(50)
                .setSaveConsumer(client::notificationBurstThreshold)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.notification_burst_stagger_ms"), client.notificationBurstStaggerMs())
                .setDefaultValue(400)
                .setMin(0)
                .setMax(10000)
                .setSaveConsumer(client::notificationBurstStaggerMs)
                .build());
        clientCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.notification_burst_max_extra_delay_ms"), client.notificationBurstMaxExtraDelayMs())
                .setDefaultValue(20000)
                .setMin(0)
                .setMax(120000)
                .setSaveConsumer(client::notificationBurstMaxExtraDelayMs)
                .build());
        clientCategory.addEntry(entries.startBooleanToggle(Component.translatable("word.banira_codex.use_custom_cursor"), client.useCustomCursor())
                .setDefaultValue(true)
                .setSaveConsumer(client::useCustomCursor)
                .build());

        ConfigCategory commonCategory = builder.getOrCreateCategory(Component.translatable("word.banira_codex.common_config"));
        commonCategory.addEntry(entries.startStrField(Component.translatable("word.banira_codex.help_header"), common.help().helpHeader())
                .setDefaultValue("-----==== Banira Codex Help (%d/%d) ====-----")
                .setSaveConsumer(common.help()::helpHeader)
                .build());
        commonCategory.addEntry(entries.startIntField(Component.translatable("word.banira_codex.help_info_num_per_page"), common.help().helpInfoNumPerPage())
                .setDefaultValue(10)
                .setMin(1)
                .setMax(100)
                .setSaveConsumer(common.help()::helpInfoNumPerPage)
                .build());
        commonCategory.addEntry(entries.startStrField(Component.translatable("word.banira_codex.default_language"), common.language().defaultLanguage())
                .setDefaultValue("en_us")
                .setSaveConsumer(common.language()::defaultLanguage)
                .build());
        commonCategory.addEntry(entries.startStrField(Component.translatable("word.banira_codex.command_prefix"), common.command().commandPrefix())
                .setDefaultValue("banira")
                .setSaveConsumer(common.command()::commandPrefix)
                .build());

        builder.setSavingRunnable(() -> {
            ClientConfig.save();
            CommonConfig.save();
        });
        return builder.build();
    }
}
