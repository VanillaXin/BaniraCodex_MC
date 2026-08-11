package xin.vanilla.banira.internal.neoforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

/**
 * 隔离 NeoForge 网络适配中的客户端连接访问，避免服务端加载通道表时解析客户端类型。
 */
public final class NeoForgeClientNetworkAccess {
    private NeoForgeClientNetworkAccess() {
    }

    public static Player player() {
        return BaniraClientRuntime.player();
    }

    public static boolean hasChannel(ResourceLocation channel) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null
                && NetworkRegistry.hasChannel(minecraft.getConnection(), channel);
    }
}
