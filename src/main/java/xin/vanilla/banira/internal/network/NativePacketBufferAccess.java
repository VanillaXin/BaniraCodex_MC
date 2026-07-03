package xin.vanilla.banira.internal.network;

/**
 * 内部原生 buffer 逃逸口，仅供版本/加载器适配复杂原版序列化。
 */
public interface NativePacketBufferAccess<T> {
    T nativeBuffer();
}
