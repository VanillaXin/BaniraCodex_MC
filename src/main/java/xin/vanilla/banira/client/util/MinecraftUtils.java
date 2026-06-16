package xin.vanilla.banira.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

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
        return BaniraClientRuntime.serverIp();
    }
}
