package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

/**
 * 隔离 Forge 网络适配中的客户端连接访问，避免服务端加载通道表时解析客户端类型。
 */
public final class ForgeClientNetworkAccess {
    private ForgeClientNetworkAccess() {
    }

    public static Player player() {
        return BaniraClientRuntime.player();
    }

    public static boolean isRemotePresent(SimpleChannel channel) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
                && channel.isRemotePresent(minecraft.getConnection().getConnection());
    }
}
