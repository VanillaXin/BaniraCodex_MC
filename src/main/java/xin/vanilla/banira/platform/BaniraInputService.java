package xin.vanilla.banira.platform;

import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;

import javax.annotation.Nonnull;

/**
 * 当前加载器的客户端输入服务。
 */
public interface BaniraInputService {
    @Nonnull
    BaniraKeyHandle register(@Nonnull BaniraKeySpec spec);

    /**
     * 将静态初始化期间暂存的按键提交给加载器。
     */
    void flushPendingRegistrations();
}
