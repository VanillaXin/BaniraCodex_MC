package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.event.BaniraGuiOverlayEvent;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void banira$beforeTabListRender(GuiGraphics guiGraphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        BaniraClientEventHub.Client.fireRenderOverlayPre(
                new BaniraGuiOverlayEvent.Pre(guiGraphics, Minecraft.getInstance().getFrameTime(), BaniraGuiOverlayEvent.PLAYER_LIST)
        );
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void banira$afterTabListRender(GuiGraphics guiGraphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
        BaniraClientEventHub.Client.fireRenderOverlayPost(
                new BaniraGuiOverlayEvent.Post(guiGraphics, Minecraft.getInstance().getFrameTime(), BaniraGuiOverlayEvent.PLAYER_LIST));
    }
}
