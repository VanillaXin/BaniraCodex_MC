package xin.vanilla.banira.internal.mixin.compat.minimap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.draw.DrawStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xin.vanilla.banira.internal.compat.minimap.JourneyMapTextBounds;

@Pseudo
@Mixin(targets = "journeymap.client.render.draw.DrawUtil", remap = false)
public abstract class JourneyMapTextBoundsMixin {
    @ModifyVariable(method = "drawBatchLabel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;Ljourneymap/client/render/draw/DrawStep$Pass;Lnet/minecraft/client/renderer/MultiBufferSource;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0, remap = false)
    private static double banira$boundInformationBatch(double scale, GuiGraphics graphics, Component text,
                                                       DrawStep.Pass pass, MultiBufferSource buffers,
                                                       double x, double y, DrawUtil.HAlign horizontal,
                                                       DrawUtil.VAlign vertical, int background,
                                                       float backgroundAlpha, int foreground, float foregroundAlpha,
                                                       double originalScale, boolean shadow, double depth) {
        return JourneyMapTextBounds.scale(text.getString(), scale);
    }
}
