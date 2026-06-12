package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.client.util.BaniraKeyBindings;

import javax.annotation.Nonnull;

/**
 * 16.5 分支的 API 输入桥接；旧键位实现留在内部适配层。
 */
public final class BaniraApiInputBridge {

    private BaniraApiInputBridge() {
    }

    @Nonnull
    public static BaniraKeyHandle register(@Nonnull BaniraKeySpec spec, @Nonnull String category) {
        return new LegacyKeyHandle(BaniraKeyBindings.register(spec.modId(), spec.suffix(), spec.defaultKey(), category));
    }

    public static void flushPendingRegistrations() {
        BaniraKeyBindings.flushPendingRegistrations();
    }

    private static final class LegacyKeyHandle implements BaniraKeyHandle {
        private final xin.vanilla.banira.client.util.BaniraKeyHandle delegate;

        private LegacyKeyHandle(xin.vanilla.banira.client.util.BaniraKeyHandle delegate) {
            this.delegate = delegate;
        }

        @Nonnull
        @Override
        public String descriptionId() {
            return delegate.descriptionId();
        }

        @Nonnull
        @Override
        public String category() {
            return delegate.categoryId();
        }

        @Override
        public int defaultKey() {
            return delegate.defaultKey();
        }

        @Override
        public boolean isDown() {
            return delegate.isDown();
        }

        @Override
        public boolean consumeClick() {
            return delegate.consumeClick();
        }
    }
}
