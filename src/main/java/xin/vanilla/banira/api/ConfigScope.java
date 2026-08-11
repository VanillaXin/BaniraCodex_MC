package xin.vanilla.banira.api;

/**
 * Banira 配置作用域。各加载器适配层负责映射到底层配置类型。
 */
public enum ConfigScope {
    COMMON,
    CLIENT,
    SERVER
}
