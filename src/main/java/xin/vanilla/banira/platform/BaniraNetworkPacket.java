package xin.vanilla.banira.platform;

/**
 * Banira 网络包公开标记接口；加载器 adapter 只按这个稳定类型发送。
 */
public interface BaniraNetworkPacket {
    /**
     * 目标网络 channel；返回 null 时使用 Banira 默认 channel。
     */
    default String channelId() {
        return null;
    }
}
