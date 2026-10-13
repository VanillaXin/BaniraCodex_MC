package xin.vanilla.banira.internal.mixin.compat.minimap;

import journeymap.client.render.backend.RenderContext;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xin.vanilla.banira.internal.fabric.compat.minimap.JourneyMapTextBounds;

@Pseudo
@Mixin(targets = "journeymap.client.render.draw.DrawUtil", remap = false)
public abstract class JourneyMapTextBoundsMixin {
    @ModifyVariable(method = "drawBatchLabel(Ljourneymap/client/render/backend/RenderContext;Ljava/lang/String;Ljourneymap/client/render/draw/DrawStep$Pass;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0, remap = false)
    private static double banira$boundInformationBatch(double scale, RenderContext context, String text, DrawStep.Pass pass,
                                                       double x, double y, DrawUtil.HAlign horizontal, DrawUtil.VAlign vertical,
                                                       int background, float backgroundAlpha, int foreground,
                                                       float foregroundAlpha, double originalScale, boolean shadow, double depth) {
        return JourneyMapTextBounds.scale(text, scale);
    }
}
