package xin.vanilla.banira.internal.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.util.BaniraEventBus;

/**
 * Game事件处理器
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        BaniraEventBus.fireClientGuiChanged();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (BaniraCodex.MODID.equals(event.getMap().location().getNamespace())) {
            BaniraEventBus.fireClientTextureReload();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        NotificationManager.get().render(event.getMatrixStack());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL
                && net.minecraft.client.Minecraft.getInstance().screen == null) {
            NotificationManager.get().render(event.getMatrixStack());
        }
    }

}
