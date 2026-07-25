package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * 将无界面通知注册为 Forge 1.21.1 HUD 的最上层。
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeNotificationLayerRegistrar {
    private static final ResourceLocation NOTIFICATION_LAYER =
            ResourceLocation.fromNamespaceAndPath(BaniraCodex.MODID, "notifications");

    private ForgeNotificationLayerRegistrar() {
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(NOTIFICATION_LAYER, (graphics, deltaTracker) -> {
            if (Minecraft.getInstance().screen == null) {
                NotificationManager.get().render(graphics);
            }
        });
    }
}
