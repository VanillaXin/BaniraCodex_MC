package xin.vanilla.banira.common.network;

import org.junit.Test;
import xin.vanilla.banira.common.network.packet.RequestToBoth;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class RequestPacketHandlersTest {

    @Test
    public void registrationTokenRemovesCurrentHandler() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        RequestHandlerRegistration registration = handlers.register(7, (packet, player) -> {
        });

        assertTrue(handlers.hasHandler(7));

        registration.close();

        assertFalse(handlers.hasHandler(7));
    }

    @Test
    public void oldRegistrationTokenDoesNotRemoveReplacement() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        RequestHandlerRegistration oldRegistration = handlers.register(7, (packet, player) -> {
        });
        RequestHandlerRegistration newRegistration = handlers.register(7, (packet, player) -> {
        });

        oldRegistration.close();

        assertTrue(handlers.hasHandler(7));

        newRegistration.close();

        assertFalse(handlers.hasHandler(7));
    }

    @Test
    public void dispatchWithoutSenderIsIgnored() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        AtomicInteger calls = new AtomicInteger();
        handlers.register(7, (packet, player) -> calls.incrementAndGet());

        assertFalse(handlers.dispatch(new RequestToBoth(7), null));
        assertEquals(0, calls.get());
    }

    @Test
    public void clearRemovesAllHandlers() {
        RequestPacketHandlers handlers = new RequestPacketHandlers();
        handlers.register(7, (packet, player) -> {
        });
        handlers.register(8, (packet, player) -> {
        });

        handlers.clear();

        assertFalse(handlers.hasHandler(7));
        assertFalse(handlers.hasHandler(8));
    }
}
