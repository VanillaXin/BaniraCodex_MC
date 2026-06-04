package xin.vanilla.banira.api.client;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.GLFWKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 跨加载器按键注册描述，只保存 Banira 自己的稳定字段。
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
public final class BaniraKeySpec {
    private @Nonnull String modId = "";
    private @Nonnull String suffix = "";
    private int defaultKey = GLFWKey.GLFW_KEY_UNKNOWN;
    private @Nullable String category;

    @Nonnull
    public BaniraKeyHandle register() {
        return BaniraInput.registerKey(this);
    }
}
