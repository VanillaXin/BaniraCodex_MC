package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.api.client.event.BaniraTextureReloadEvent;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;

@Mixin(TextureAtlas.class)
public abstract class AuiTextureAtlasMixin {
    @Shadow
    public abstract Identifier location();

    @Inject(method = "upload(Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;)V", at = @At("TAIL"))
    private void banira$textureReloaded(CallbackInfo callback) {
        BaniraClientEventHub.Client.fireTextureReload(new BaniraTextureReloadEvent(location().toString()));
    }
}
