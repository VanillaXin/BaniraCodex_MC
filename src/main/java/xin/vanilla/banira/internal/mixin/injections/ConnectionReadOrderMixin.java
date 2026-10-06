package xin.vanilla.banira.internal.mixin.injections;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Connection.class, priority = 900)
public abstract class ConnectionReadOrderMixin {
    @Shadow
    private Channel channel;

    @Redirect(method = "sendPacket", at = @At(value = "INVOKE",
            target = "Lio/netty/channel/ChannelConfig;setAutoRead(Z)Lio/netty/channel/ChannelConfig;",
            remap = false), require = 0)
    private ChannelConfig banira$setAutoReadInOrder(ChannelConfig config, boolean autoRead) {
        // Keep NIO read-interest changes ordered with protocol transitions on the event loop.
        if (channel.eventLoop().inEventLoop()) {
            config.setAutoRead(autoRead);
        } else {
            channel.eventLoop().execute(() -> config.setAutoRead(autoRead));
        }
        return config;
    }
}
