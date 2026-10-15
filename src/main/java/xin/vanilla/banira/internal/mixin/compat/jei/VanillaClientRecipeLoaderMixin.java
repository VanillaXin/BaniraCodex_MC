package xin.vanilla.banira.internal.mixin.compat.jei;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "mezz.jei.common.recipes.VanillaClientRecipeLoader", remap = false)
public abstract class VanillaClientRecipeLoaderMixin {
    @ModifyArg(method = "openVanillaServerDataPack()Lnet/minecraft/server/packs/PackResources;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/ServerPacksSource;loadPacks(Ljava/util/function/Consumer;Lnet/minecraft/server/packs/repository/Pack$PackConstructor;)V",
                    remap = true), index = 1, remap = false)
    private static Pack.PackConstructor banira$forgePackConstructor(Pack.PackConstructor original) {
        return (id, title, required, resources, metadata, position, source, hidden) ->
                new Pack(id, title, required, resources, metadata, PackType.SERVER_DATA, position, source, hidden);
    }
}
