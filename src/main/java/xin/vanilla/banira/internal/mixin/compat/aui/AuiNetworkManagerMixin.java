package xin.vanilla.banira.internal.mixin.compat.aui;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xin.vanilla.banira.internal.compat.aui.AuiOptionalServerChannels;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "com.sighs.apricityui.network.forge.NetworkManagerImpl", remap = false)
public abstract class AuiNetworkManagerMixin {
    @Redirect(method = {
            "lambda$serverboundChannelOf$1(Ljava/lang/String;)Lnet/minecraftforge/network/simple/SimpleChannel;",
            "lambda$clientboundChannelOf$3(Ljava/lang/String;)Lnet/minecraftforge/network/simple/SimpleChannel;"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/NetworkRegistry;newSimpleChannel("
            + "Lnet/minecraft/resources/ResourceLocation;Ljava/util/function/Supplier;Ljava/util/function/Predicate;"
            + "Ljava/util/function/Predicate;)Lnet/minecraftforge/network/simple/SimpleChannel;"), require = 2, remap = false)
    private static SimpleChannel banira$optionalServer(ResourceLocation name, Supplier<String> version,
                                                      Predicate<String> clientAccepted, Predicate<String> serverAccepted) {
        // Forge evaluates the client predicate against the server's version.
        return NetworkRegistry.newSimpleChannel(name, version,
                AuiOptionalServerChannels.clientVersions(name.getNamespace(), name.getPath(), clientAccepted,
                        NetworkRegistry.ABSENT.version()), serverAccepted);
    }
}
