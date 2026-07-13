package xin.vanilla.banira.platform;

import javax.annotation.Nullable;

/**
 * 当前 MC 版本的服务器运行时句柄服务。
 */
public interface BaniraServerService {
    @Nullable
    Object current();

    boolean isRunning();
}
