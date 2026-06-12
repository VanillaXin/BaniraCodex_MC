package xin.vanilla.banira.api.client;

import javax.annotation.Nonnull;

/**
 * 子 mod 持有的按键句柄；底层原生键位对象由当前加载器分支隐藏。
 */
public interface BaniraKeyHandle {

    @Nonnull
    String descriptionId();

    @Nonnull
    String category();

    int defaultKey();

    boolean isDown();

    boolean consumeClick();
}
