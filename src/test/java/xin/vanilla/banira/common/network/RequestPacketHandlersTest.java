package xin.vanilla.banira.common.network;

import org.junit.Test;
import xin.vanilla.banira.common.network.packet.RequestToBoth;

import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class RequestPacketHandlersTest {

    @Test
    public void registerAndUnregisterByType() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        handlers.register(7, (packet, player) -> {
        });

        assertTrue(handlers.hasHandler(7));
        assertTrue(handlers.unregister(7));
        assertFalse(handlers.hasHandler(7));
    }

    @Test
    public void registrationOnlyRemovesOwnHandler() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        BiConsumer<RequestToBoth, Object> first = (packet, player) -> {
        };
        BiConsumer<RequestToBoth, Object> second = (packet, player) -> {
        };

        RequestHandlerRegistration registration = handlers.register(3, first);
        handlers.register(3, second);
        registration.close();

        assertTrue(handlers.hasHandler(3));
    }

    @Test
    public void dispatchWithoutPlayerIsIgnored() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        handlers.register(2, (packet, player) -> {
            throw new AssertionError("handler should not run without server player");
        });

        assertFalse(handlers.dispatch(new RequestToBoth(2), null));
    }
}
