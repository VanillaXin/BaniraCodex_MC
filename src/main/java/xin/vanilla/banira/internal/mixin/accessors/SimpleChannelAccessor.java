package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraftforge.network.NetworkInstance;
import net.minecraftforge.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SimpleChannel.class, remap = false)
public interface SimpleChannelAccessor {

    @Accessor(value = "instance")
    NetworkInstance banira$instance();

}
