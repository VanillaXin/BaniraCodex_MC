package xin.vanilla.banira.platform;

import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;

/** 测试用输入服务，避免平台单元测试绑定具体客户端加载器。 */
public enum NoopInputService implements BaniraInputService {
    INSTANCE;

    @Override
    public BaniraKeyHandle register(BaniraKeySpec spec) {
        throw new UnsupportedOperationException("Noop input service");
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        return false;
    }

    @Override
    public boolean isMouseDown(int button) {
        return false;
    }

    @Override
    public void flushPendingRegistrations() {
    }
}
