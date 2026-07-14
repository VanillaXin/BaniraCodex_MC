package xin.vanilla.banira.internal.neoforge.platform;

import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.platform.BaniraServerService;

/** NeoForge 1.21.1 的活动服务器句柄适配。 */
public final class NeoForgeBaniraServerService implements BaniraServerService {
    public static final NeoForgeBaniraServerService INSTANCE = new NeoForgeBaniraServerService();

    private NeoForgeBaniraServerService() {
    }

    @Override
    public Object current() {
        return BaniraServerRuntime.server();
    }

    @Override
    public boolean isRunning() {
        return BaniraServerRuntime.isRunning();
    }
}
