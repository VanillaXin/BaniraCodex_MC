package xin.vanilla.banira.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Minecraft客户端工具类
 */
@Environment(EnvType.CLIENT)
public final class MinecraftUtils {
    private MinecraftUtils() {
    }


    /**
     * 获取当前连接的服务器IP
     *
     * @return 若未连接服务器则返回空字符串
     */
    public static String getServerIp() {
        Minecraft minecraft = Minecraft.getInstance();
        ServerData currentServer = minecraft.getCurrentServer();
        return currentServer != null ? currentServer.ip : "";
    }
}
