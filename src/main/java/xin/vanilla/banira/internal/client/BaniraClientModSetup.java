package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.notification.BaniraClientNotificationTypes;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.BaniraKeyHandle;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

/**
 * Banira-owned client setup tasks invoked by the active loader adapter.
 */
public final class BaniraClientModSetup {
    public static final BaniraKeyHandle NOTIFICATION_LOG_KEY =
            BaniraKeyBindings.register(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);

    public static final BaniraKeyHandle BANIRA_HUB_KEY =
            BaniraKeyBindings.register(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    private BaniraClientModSetup() {
    }

    public static void initOnClientSetup() {
        BaniraKeyBindings.flushPendingRegistrations();
        NotificationManager.get().loadLog();
        registerOwnNotificationTypes();
        NotificationTypeSettingsStore.get().load();

        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup();
    }

    private static void registerOwnNotificationTypes() {
        BaniraClientNotificationTypes.registerModDisplayName(BaniraCodex.MODID,
                BaniraComponent.get().transClientAuto("mod_name"));
        BaniraClientNotificationTypes.register(NotificationTypeKeys.HELP,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                BaniraComponent.get().transClientAuto("notification_type_help"));
        BaniraClientNotificationTypes.register(NotificationTypeKeys.COMMAND_FEEDBACK,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                BaniraComponent.get().transClientAuto("notification_type_command_feedback"));
    }
}
