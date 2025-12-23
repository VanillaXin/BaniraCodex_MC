package xin.vanilla.banira.internal.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.util.BaniraEventBus;

/**
 * Game事件处理器
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

}
