package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkInstance;
import net.minecraftforge.fml.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = NetworkRegistry.class, remap = false)
public interface NetworkRegistryAccessor {

    @Accessor(value = "instances")
    Map<ResourceLocation, NetworkInstance> banira$instances();

}
