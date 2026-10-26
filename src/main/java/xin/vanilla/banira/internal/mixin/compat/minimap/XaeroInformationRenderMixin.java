package xin.vanilla.banira.internal.mixin.compat.minimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Retained for existing mixin configurations; Xaero owns all information rendering. */
@Deprecated
@Pseudo
@Mixin(targets = "xaero.hud.minimap.info.render.InfoDisplayRenderer", remap = false)
public abstract class XaeroInformationRenderMixin {
}
