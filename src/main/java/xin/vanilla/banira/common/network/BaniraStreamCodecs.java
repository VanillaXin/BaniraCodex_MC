package xin.vanilla.banira.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * NeoForge 原生 payload 的过渡编码适配器；优先使用 {@link xin.vanilla.banira.api.BaniraNetwork#registrar}。
 */
public final class BaniraStreamCodecs {
    private BaniraStreamCodecs() {
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> registryBuf(
            BiConsumer<T, RegistryFriendlyByteBuf> encoder,
            Function<RegistryFriendlyByteBuf, T> decoder) {
        return new StreamCodec<>() {
            @Nonnull
            @Override
            public T decode(@Nonnull RegistryFriendlyByteBuf buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull T value) {
                encoder.accept(value, buf);
            }
        };
    }
}
