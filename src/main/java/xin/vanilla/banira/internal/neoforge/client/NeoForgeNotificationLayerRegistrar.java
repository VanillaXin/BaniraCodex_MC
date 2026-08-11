package xin.vanilla.banira.internal.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * 将无界面通知注册为 NeoForge 1.21.1 HUD 的最上层。
 */
@EventBusSubscriber(modid = Banira.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeNotificationLayerRegistrar {
    private static final ResourceLocation NOTIFICATION_LAYER =
            ResourceLocation.fromNamespaceAndPath(Banira.MOD_ID, "notifications");

    private NeoForgeNotificationLayerRegistrar() {
    }

    @SubscribeEvent
    public static void onAddGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(NOTIFICATION_LAYER, (graphics, deltaTracker) -> {
            if (Minecraft.getInstance().screen == null) {
                NotificationManager.get().render(graphics);
            }
        });
    }
}
