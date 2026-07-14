package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.platform.BaniraServerService;

/** Forge 1.18.2 的活动服务器句柄适配。 */
public final class ForgeBaniraServerService implements BaniraServerService {
    public static final ForgeBaniraServerService INSTANCE = new ForgeBaniraServerService();

    private ForgeBaniraServerService() {
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
