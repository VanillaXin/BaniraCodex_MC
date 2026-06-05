package xin.vanilla.banira.common.network;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Banira 网络包处理上下文；不同加载器只需要适配这些最小语义。
 */
public interface BaniraNetworkContext {
    /**
     * 将处理逻辑切回 MC 主线程执行。
     */
    void enqueueWork(Runnable work);

    /**
     * 标记当前网络包已经处理完毕。
     */
    void markHandled();

    /**
     * 当前包是否由客户端接收。
     */
    boolean isClientSide();

    /**
     * 当前包是否由服务端接收。
     */
    boolean isServerSide();

    /**
     * 服务端收到客户端包时的发送者；客户端接收包时通常为空。
     */
    @Nullable
    ServerPlayer sender();
}
