package xin.vanilla.banira.common.network;

/**
 * Mod 安装状态声明的注册凭据；关闭后撤销本次声明。
 */
@FunctionalInterface
public interface ModLoadedRegistration extends AutoCloseable {

    ModLoadedRegistration NOOP = () -> {
    };

    @Override
    void close();
}
