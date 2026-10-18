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
import org.spongepowered.asm.mixin.injection.Redirect;
import xin.vanilla.banira.internal.compat.minimap.JourneyMapTextBounds;

@Pseudo
@Mixin(targets = "journeymap.client.render.draw.DrawUtil", remap = false)
public abstract class JourneyMapTextBoundsMixin {
    @Redirect(method = "drawBatchLabel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/network/chat/Component;Ljourneymap/client/render/draw/DrawStep$Pass;Lnet/minecraft/client/renderer/MultiBufferSource;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", remap = true), require = 0, remap = false)
    private static int banira$drawInformation(net.minecraft.client.gui.Font font, Component text, float x, float y,
                                             int color, boolean shadow, org.joml.Matrix4f matrix, MultiBufferSource buffers,
                                             net.minecraft.client.gui.Font.DisplayMode mode, int background, int light,
                                             GuiGraphics graphics, Component originalText, DrawStep.Pass pass,
                                             MultiBufferSource originalBuffers, double anchorX, double anchorY,
                                             DrawUtil.HAlign horizontal, DrawUtil.VAlign vertical, int backgroundColor,
                                             float backgroundAlpha, int foreground, float foregroundAlpha,
                                             double scale, boolean originalShadow, double rotation) {
        if (JourneyMapTextBounds.drawOwned(graphics, text, x, y, color, shadow, matrix, buffers))
            return (int) x + font.width(text);
        return font.drawInBatch(text, x, y, color, shadow, matrix, buffers, mode, background, light);
    }

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
