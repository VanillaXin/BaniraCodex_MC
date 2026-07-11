package xin.vanilla.banira.common.config;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Banira 自己的配置作用域，避免公共 API 暴露具体加载器的配置类型。
 */
public enum ConfigScope {
    COMMON,
    CLIENT,
    SERVER;

    @Nonnull
    public String extension() {
        return name().toLowerCase(Locale.ROOT);
    }
}
