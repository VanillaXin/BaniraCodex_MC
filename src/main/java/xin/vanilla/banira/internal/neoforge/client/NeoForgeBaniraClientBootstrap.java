package xin.vanilla.banira.internal.neoforge.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import xin.vanilla.banira.client.util.ExtractedGuiRenderer;
import xin.vanilla.banira.internal.compat.aui.AuiDocumentRenderSubmission;
import xin.vanilla.banira.internal.mixin.accessors.GuiGraphicsExtractorAccessor;

/**
 * NeoForge 入口与实际 GUI 启动类之间的轻量客户端桥。
 */
public final class NeoForgeBaniraClientBootstrap {
    private NeoForgeBaniraClientBootstrap() {
    }

    public static void init(IEventBus modBus) {
        ExtractedGuiRenderer.install(graphics -> ((GuiGraphicsExtractorAccessor) graphics).banira$getGuiRenderState(),
                GuiGraphicsExtractor::peekScissorStack);
        if (ModList.get().isLoaded("apricityui")) {
            modBus.addListener((RegisterPictureInPictureRenderersEvent event) -> AuiDocumentRenderSubmission.register(event));
        }
        modBus.addListener(BaniraClientModSetup::onClientSetup);
        modBus.addListener(BaniraClientModSetup::onRegisterKeyMappings);
        modBus.addListener(BaniraClientModSetup::onRegisterReloadListeners);
        modBus.addListener(BaniraClientModSetup::onTextureAtlasStitched);
        modBus.addListener(NeoForgeNotificationLayerRegistrar::onAddGuiLayers);
        xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap.init();
        xin.vanilla.banira.internal.neoforge.compat.NeoForgeExternalInventoryCompatibility.init();
    }
}
