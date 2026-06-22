package xin.vanilla.banira.internal.fabric.network;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.network.BaniraNetworkContext;

import javax.annotation.Nullable;

/**
 * Fabric 网络回调上下文；负责把处理逻辑切回 MC 主线程。
 */
public final class FabricNetworkContext implements BaniraNetworkContext {
    private final MinecraftServer server;
    private final ServerPlayer sender;
    private final boolean clientSide;

    private FabricNetworkContext(MinecraftServer server, ServerPlayer sender, boolean clientSide) {
        this.server = server;
        this.sender = sender;
        this.clientSide = clientSide;
    }

    public static FabricNetworkContext client() {
        return new FabricNetworkContext(null, null, true);
    }

    public static FabricNetworkContext server(MinecraftServer server, ServerPlayer sender) {
        return new FabricNetworkContext(server, sender, false);
    }

    @Override
    public void enqueueWork(Runnable work) {
        if (clientSide) {
            Minecraft.getInstance().execute(work);
        } else if (server != null) {
            server.execute(work);
        } else {
            work.run();
        }
    }

    @Override
    public void markHandled() {
    }

    @Override
    public boolean isClientSide() {
        return clientSide;
    }

    @Override
    public boolean isServerSide() {
        return !clientSide;
    }

    @Nullable
    @Override
    public Object sender() {
        return sender;
    }
}
