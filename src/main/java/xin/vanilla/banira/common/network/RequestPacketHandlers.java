package xin.vanilla.banira.common.network;

import xin.vanilla.banira.common.network.packet.RequestToBoth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * RequestToBoth 的处理器注册表，隔离静态包类型与可替换的请求处理逻辑。
 */
public final class RequestPacketHandlers {

    private final Object lock = new Object();
    private final Map<Integer, BiConsumer<RequestToBoth, Object>> handlers = new LinkedHashMap<>();

    /**
     * 注册或覆盖一个请求类型处理器，并返回可撤销本次注册的凭据。
     */
    @Nonnull
    public RequestHandlerRegistration register(int requestType,
                                               @Nonnull BiConsumer<RequestToBoth, Object> handler) {
        Objects.requireNonNull(handler, "handler");
        synchronized (lock) {
            handlers.put(requestType, handler);
        }
        return () -> unregister(requestType, handler);
    }

    /**
     * 仅当当前处理器仍是指定实例时才移除，避免旧凭据误删后续覆盖注册。
     */
    public boolean unregister(int requestType, @Nonnull BiConsumer<RequestToBoth, Object> handler) {
        Objects.requireNonNull(handler, "handler");
        synchronized (lock) {
            if (handlers.get(requestType) == handler) {
                handlers.remove(requestType);
                return true;
            }
            return false;
        }
    }

    public boolean unregister(int requestType) {
        synchronized (lock) {
            return handlers.remove(requestType) != null;
        }
    }

    public boolean hasHandler(int requestType) {
        synchronized (lock) {
            return handlers.containsKey(requestType);
        }
    }

    public boolean dispatch(@Nonnull RequestToBoth packet, @Nullable Object player) {
        if (player == null) {
            return false;
        }
        BiConsumer<RequestToBoth, Object> handler;
        synchronized (lock) {
            handler = handlers.get(packet.requestType());
        }
        if (handler == null) {
            return false;
        }
        handler.accept(packet, player);
        return true;
    }

    public void clear() {
        synchronized (lock) {
            handlers.clear();
        }
    }
}
