package xin.vanilla.banira.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Minecraft客户端工具类
 */
@OnlyIn(Dist.CLIENT)
public final class MinecraftUtils {

    private MinecraftUtils() {
    }

    /**
     * 获取当前连接的服务器IP
     *
     * @return 若未连接服务器则返回空字符串
     */
    public static String getServerIP() {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData currentServer = minecraft.getCurrentServer();
        return currentServer != null ? currentServer.ip : "";
    }
}
