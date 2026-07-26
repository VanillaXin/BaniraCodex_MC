package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.16 客户端设置包未公开语言字段。
 */
@Mixin(ServerboundClientInformationPacket.class)
public interface ClientInformationPacketAccessor {
    @Accessor("language")
    String banira$language();
}
