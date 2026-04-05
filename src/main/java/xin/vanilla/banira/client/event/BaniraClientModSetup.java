package xin.vanilla.banira.client.event;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * 客户端 Mod 总线（{@code Dist.CLIENT}）：键位注册、通知日志加载、{@link BaniraClientEventHub} 默认回调与 {@link FMLClientSetupEvent} 分发。
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BaniraClientModSetup {

    public static final KeyMapping NOTIFICATION_LOG_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);

    private BaniraClientModSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BaniraKeyBindings.flushPendingRegistrations();
        NotificationManager.get().loadLog();
        NotificationTypeSettingsStore.get().load();

        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup(event);
    }
}
