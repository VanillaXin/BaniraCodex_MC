package xin.vanilla.banira.internal.event;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.gui.quickaction.InventoryQuickActionOverlay;
import xin.vanilla.banira.common.util.BaniraEventBus;

/**
 * 客户端 Forge 事件转发器
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GameEventHandler {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        InventoryQuickActionOverlay.get().resetInteractionState();
        BaniraEventBus.Client.fireGuiChanged(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        BaniraEventBus.Client.fireTextureReload(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (InventoryQuickActionOverlay.isSupportedInventoryScreen(event.getGui())) {
            InventoryQuickActionOverlay.get().tickInteraction(event.getGui(), event.getMouseX(), event.getMouseY());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        BaniraEventBus.Client.fireDrawScreenPost(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreenPostInventoryQuickAction(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (InventoryQuickActionOverlay.isSupportedInventoryScreen(event.getGui())) {
            InventoryQuickActionOverlay.get().render(event.getMatrixStack(), event.getGui(), event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
            InventoryQuickActionOverlay.get().flushSaveIfNeeded();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (InventoryQuickActionOverlay.get().handleMouseClicked(event.getGui(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(GuiScreenEvent.MouseReleasedEvent.Pre event) {
        if (InventoryQuickActionOverlay.get().handleMouseReleased(event.getGui(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(GuiScreenEvent.MouseScrollEvent.Pre event) {
        if (InventoryQuickActionOverlay.get().handleMouseScroll(event.getGui(), event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        BaniraEventBus.Client.fireRenderOverlayPost(event);
    }

}
