package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraftforge.fmllegacy.network.NetworkInstance;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SimpleChannel.class, remap = false)
public interface SimpleChannelAccessor {

    @Accessor(value = "instance")
    NetworkInstance banira$instance();

}
