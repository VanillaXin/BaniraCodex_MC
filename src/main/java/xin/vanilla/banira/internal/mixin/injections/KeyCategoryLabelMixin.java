package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.neoforge.client.NeoForgeKeyBindingService;

/**
 * Keeps registered Banira categories on their existing translation keys.
 */
@Mixin(KeyMapping.Category.class)
public abstract class KeyCategoryLabelMixin {
    @Shadow
    public abstract Identifier id();

    @Inject(method = "label()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void banira$categoryLabel(CallbackInfoReturnable<Component> callback) {
        Component label = NeoForgeKeyBindingService.categoryLabel(id());
        if (label != null) callback.setReturnValue(label);
    }
}
