package xin.vanilla.banira;

import lombok.NonNull;
import xin.vanilla.banira.common.data.AbstractComponent;

/**
 * BaniraCodex 本 Mod 的 {@link xin.vanilla.banira.common.data.Component} 构建入口。
 */
public final class BaniraComponent extends AbstractComponent {

    public static final BaniraComponent INSTANCE = new BaniraComponent();

    private BaniraComponent() {
    }

    @Override
    protected @NonNull String modId() {
        return BaniraCodex.MODID;
    }

    public static BaniraComponent get() {
        return INSTANCE;
    }
}
