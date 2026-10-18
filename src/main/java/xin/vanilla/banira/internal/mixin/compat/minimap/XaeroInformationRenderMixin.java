package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.module.MinimapSession;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.ui.MinimapUiRows;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.info.render.InfoDisplayRenderer", remap = false)
public abstract class XaeroInformationRenderMixin {
    @Redirect(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/minimap/Minimap;IILnet/minecraft/core/BlockPos;IIFLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V",
            at = @At(value = "INVOKE", target = "Lxaero/common/misc/Misc;drawNormalText(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;FFIZLnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V", remap = false), require = 0, remap = false)
    private void banira$drawInformation(PoseStack pose, Component text, float x, float y, int color, boolean shadow,
                                        MultiBufferSource.BufferSource buffers, GuiGraphics graphics,
                                        MinimapSession session, Minimap minimap, int scaledHeight, int width,
                                        BlockPos pos, int mapX, int mapY, float scale, MultiBufferSource.BufferSource originalBuffers) {
        if (!MinimapUiRows.drawOwned(EnumNotificationHudHost.XAERO, graphics, text,
                new ScreenCoordinate(x, y, Minecraft.getInstance().font.width(text), Minecraft.getInstance().font.lineHeight),
                color, shadow, buffers::endBatch))
            xaero.common.misc.Misc.drawNormalText(pose, text, x, y, color, shadow, buffers);
    }
}
