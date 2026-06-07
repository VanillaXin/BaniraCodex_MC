package xin.vanilla.banira.common.network;

/**
 * 请求处理器注册凭据；关闭后只移除自己对应的那次注册。
 */
@FunctionalInterface
public interface RequestHandlerRegistration extends AutoCloseable {

    @Override
    void close();
}
