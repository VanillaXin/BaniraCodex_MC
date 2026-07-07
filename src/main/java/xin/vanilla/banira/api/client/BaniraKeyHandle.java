package xin.vanilla.banira.api.client;

import javax.annotation.Nonnull;

/**
 * 子 mod 持有的按键句柄；底层 KeyMapping/KeyBinding 由当前加载器分支隐藏。
 */
public interface BaniraKeyHandle {

    @Nonnull
    String descriptionId();

    @Nonnull
    String category();

    int defaultKey();

    /**
     * 当前实际绑定键码；用于 GUI 内按键事件与用户改键后的配置保持一致。
     */
    int currentKey();

    boolean isDown();

    boolean consumeClick();
}
