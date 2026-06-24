package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;
import xin.vanilla.banira.internal.client.BaniraClientDrawBridge;
import xin.vanilla.banira.internal.client.BaniraClientOverlayBridge;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.LogoModifier;
import xin.vanilla.banira.internal.forge.util.ForgeLogoModifier;

/**
 * 客户端 Mod 总线（{@code Dist.CLIENT}）：键位注册、通知日志加载、{@link BaniraClientEventHub} 默认回调与 {@link FMLClientSetupEvent} 分发。
 */
@Mod.EventBusSubscriber(modid = Banira.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BaniraClientModSetup {

    public static final BaniraKeyHandle NOTIFICATION_LOG_KEY = BaniraInput.registerKey(Banira.MOD_ID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);

    public static final BaniraKeyHandle BANIRA_HUB_KEY = BaniraInput.registerKey(Banira.MOD_ID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    private static boolean codexScreenKeyCallbackRegistered;

    private BaniraClientModSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BaniraClientDrawBridge.install(ForgeBaniraDrawHandle::new);
        LogoModifier.installApplier(ForgeLogoModifier::modifyLogo);
        BaniraInput.flushPendingRegistrations();
        BaniraClientOverlayBridge.loadNotificationLog();
        NotificationTypeSettingsStore.get().load();

        BaniraClientEventHub.registerCodexDefaults();
        registerCodexScreenKeyCallback();
        BaniraClientEventHub.dispatchModClientSetup(new BaniraClientSetupEvent());
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ForgeKeyBindingService.INSTANCE.flushPendingRegistrations(event);
    }

    private static void registerCodexScreenKeyCallback() {
        if (codexScreenKeyCallbackRegistered) {
            return;
        }
        codexScreenKeyCallbackRegistered = true;
        BaniraClientEventHub.Client.onClientTick(BaniraClientModSetup::openCodexScreenByKey);
    }

    private static void openCodexScreenByKey(BaniraClientTickEvent event) {
        if (event != BaniraClientTickEvent.END || BaniraClientRuntime.currentScreen() != null) {
            return;
        }
        if (NOTIFICATION_LOG_KEY.isDown()) {
            BaniraClientRuntime.setScreen(new NotificationLogScreen(null));
        } else if (BANIRA_HUB_KEY.isDown()) {
            BaniraClientRuntime.setScreen(new CodexNavigationScreen(null));
        }
    }
}
