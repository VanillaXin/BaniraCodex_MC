package xin.vanilla.banira.internal.event;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * Mod事件处理器
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CATEGORIES = String.format("key.%s.categories", BaniraCodex.MODID);

    public static KeyBinding DEBUG_KEY = new KeyBinding(
            String.format("key.%s.debug", BaniraCodex.MODID),
            GLFWKey.GLFW_KEY_UNKNOWN,
            CATEGORIES
    );

    /**
     * 注册键绑定
     */
    @SubscribeEvent
    public static void registerKeyBindings(FMLClientSetupEvent event) {
        ClientRegistry.registerKeyBinding(DEBUG_KEY);
        NotificationManager.get().loadLog();
    }
}
