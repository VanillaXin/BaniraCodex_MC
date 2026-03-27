package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.network.NetworkInstance;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = NetworkRegistry.class, remap = false)
public interface NetworkRegistryAccessor {

    @Accessor(value = "instances")
    Map<ResourceLocation, NetworkInstance> banira$instances();

}
