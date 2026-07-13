package xin.vanilla.banira.api.event;

import org.junit.Test;
import xin.vanilla.banira.common.util.BaniraEventBus;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class BaniraEventsTest {
    @Test
    public void registrationCanBeUnregisteredWithoutLoaderTypes() {
        AtomicInteger calls = new AtomicInteger();
        BaniraEventRegistration registration = BaniraEvents.Server.onTick(event -> calls.incrementAndGet());

        BaniraEventBus.dispatchServerTick(new BaniraServerEvent(null));
        registration.unregister();
        BaniraEventBus.dispatchServerTick(new BaniraServerEvent(null));

        assertEquals(1, calls.get());
    }
}
