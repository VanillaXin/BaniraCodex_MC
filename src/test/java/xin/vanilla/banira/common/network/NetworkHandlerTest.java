package xin.vanilla.banira.common.network;

import org.junit.Test;
import xin.vanilla.banira.common.api.INetworkPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class NetworkHandlerTest {
    @Test
    public void registerUsesSequentialPacketIds() {
        List<Integer> packetIds = new ArrayList<>();
        NetworkHandler handler = new NetworkHandler(new NetworkPacketRegistrar() {
            @Override
            public <MSG extends INetworkPacket> void register(
                    int packetId,
                    Class<MSG> packetClass,
                    BiConsumer<MSG, BaniraPacketBuffer> encoder,
                    Function<BaniraPacketBuffer, MSG> decoder,
                    BiConsumer<MSG, BaniraNetworkContext> receiver) {
                packetIds.add(packetId);
            }
        });

        handler.register(FirstPacket.class, (packet, buffer) -> {
        }, buffer -> new FirstPacket(), (packet, context) -> {
        });
        handler.register(SecondPacket.class, (packet, buffer) -> {
        }, buffer -> new SecondPacket(), (packet, context) -> {
        });

        assertEquals(Integer.valueOf(0), packetIds.get(0));
        assertEquals(Integer.valueOf(1), packetIds.get(1));
    }

    private static final class FirstPacket implements INetworkPacket {
    }

    private static final class SecondPacket implements INetworkPacket {
    }
}
