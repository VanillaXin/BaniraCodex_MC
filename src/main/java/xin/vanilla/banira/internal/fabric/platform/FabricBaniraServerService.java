package xin.vanilla.banira.internal.fabric.platform;

import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.platform.BaniraServerService;

import javax.annotation.Nullable;

/** Fabric 1.18.2 的服务器运行时适配。 */
public final class FabricBaniraServerService implements BaniraServerService {
    public static final FabricBaniraServerService INSTANCE = new FabricBaniraServerService();

    private FabricBaniraServerService() {
    }

    @Nullable
    @Override
    public Object current() {
        return BaniraServerRuntime.server();
    }

    @Override
    public boolean isRunning() {
        return BaniraServerRuntime.isRunning();
    }
}
