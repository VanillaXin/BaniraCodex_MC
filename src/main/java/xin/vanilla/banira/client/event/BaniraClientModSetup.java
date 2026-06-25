package xin.vanilla.banira.client.event;

import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.BaniraKeyBindings;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * 客户端 Mod 总线（{@code Dist.CLIENT}）：键位注册、通知日志加载、{@link BaniraClientEventHub} 默认回调与 {@link FMLClientSetupEvent} 分发。
 */
public final class BaniraClientModSetup {

    public static final KeyMapping NOTIFICATION_LOG_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "notification_log", GLFWKey.GLFW_KEY_UNKNOWN);

    public static final KeyMapping BANIRA_HUB_KEY = BaniraKeyBindings.register(BaniraCodex.MODID, "codex_navigation", GLFWKey.GLFW_KEY_UNKNOWN);

    private BaniraClientModSetup() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterKeyMappingsEvent.class, BaniraClientModSetup::onRegisterKeyMappings);
        modEventBus.addListener(TextureAtlasStitchedEvent.class, BaniraClientModSetup::onTextureAtlasStitched);
        modEventBus.addListener(FMLClientSetupEvent.class, BaniraClientModSetup::onClientSetup);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        BaniraKeyBindings.flushPendingRegistrations(event);
    }

    @SubscribeEvent
    public static void onTextureAtlasStitched(TextureAtlasStitchedEvent event) {
        BaniraClientEventHub.Client.fireTextureReload(event);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NotificationManager.get().loadLog();
        NotificationTypeSettingsStore.get().load();

        BaniraClientEventHub.registerCodexDefaults();
        BaniraClientEventHub.dispatchModClientSetup(event);
    }
}
