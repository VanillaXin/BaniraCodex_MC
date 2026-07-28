package xin.vanilla.banira.client.event;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

/**
 * 客户端 Mod 总线（{@code Dist.CLIENT}）：键位注册、通知日志加载、{@link BaniraClientEventHub} 默认回调与 {@link FMLClientSetupEvent} 分发。
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BaniraClientModSetup {

    public static final KeyMapping NOTIFICATION_LOG_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);

    public static final KeyMapping BANIRA_HUB_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    private BaniraClientModSetup() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        BaniraKeyBindings.flushPendingRegistrations(event);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        registerOwnNotificationTypes();
        NotificationManager.get().loadLog();
        NotificationTypeSettingsStore.get().load();

        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup(event);
    }

    private static void registerOwnNotificationTypes() {
        NotificationTypeRegistry.registerModDisplayNameInternal(BaniraCodex.MODID,
                BaniraComponent.get().transClientAuto("mod_name"));
        NotificationTypeRegistry.registerInternal(NotificationTypeKeys.HELP,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                BaniraComponent.get().transClientAuto("notification_type_help"));
        NotificationTypeRegistry.registerInternal(NotificationTypeKeys.COMMAND_FEEDBACK,
                EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                BaniraComponent.get().transClientAuto("notification_type_command_feedback"));
    }
}
