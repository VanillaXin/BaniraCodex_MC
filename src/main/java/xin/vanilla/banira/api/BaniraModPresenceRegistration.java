package xin.vanilla.banira.api;

/**
 * 客户端可选 mod 上报声明的注册句柄；关闭后撤销本次声明。
 */
@FunctionalInterface
public interface BaniraModPresenceRegistration extends AutoCloseable {

    BaniraModPresenceRegistration NOOP = () -> {
    };

    @Override
    void close();
}
