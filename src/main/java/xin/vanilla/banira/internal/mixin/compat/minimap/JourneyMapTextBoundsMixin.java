package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import journeymap.client.render.draw.DrawUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Desc;
import xin.vanilla.banira.internal.compat.minimap.JourneyMapTextBounds;

@Pseudo
@Mixin(targets = "journeymap.client.render.draw.DrawUtil", remap = false)
public abstract class JourneyMapTextBoundsMixin {
    @ModifyVariable(target = @Desc(value = "drawBatchLabel", args = {PoseStack.class, Component.class,
            MultiBufferSource.class, double.class, double.class, DrawUtil.HAlign.class, DrawUtil.VAlign.class,
            Integer.class, float.class, int.class, float.class, double.class, boolean.class, double.class}),
            at = @At("HEAD"), argsOnly = true, ordinal = 2, require = 0, remap = false)
    private static double banira$boundInformationBatch(double scale, PoseStack stack, Component text,
                                                       MultiBufferSource buffers, double x, double y,
                                                       DrawUtil.HAlign horizontal, DrawUtil.VAlign vertical,
                                                       Integer background, float backgroundAlpha, int foreground,
                                                       float foregroundAlpha, double originalScale, boolean shadow, double depth) {
        return JourneyMapTextBounds.scale(text.getString(), scale);
    }
}
