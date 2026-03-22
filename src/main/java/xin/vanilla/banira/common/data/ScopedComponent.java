package xin.vanilla.banira.common.data;

import lombok.NonNull;

/**
 * 按任意 modId 构建 {@link Component}，用于运行时 mod 命名空间已知但非单一固定 Mod 的场景（如 {@link xin.vanilla.banira.common.util.Translator}、配置元数据）。
 */
public final class ScopedComponent extends AbstractComponent {

    private final String modId;

    public ScopedComponent(@NonNull String modId) {
        this.modId = modId;
    }

    @Override
    protected @NonNull String modId() {
        return this.modId;
    }
}
