package xin.vanilla.banira.client.event;

import org.junit.Test;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/** 验证旧事件中心会继续派发到推荐的客户端公共 API。 */
public class BaniraClientEventHubTest {
    @Test
    public void clientTickReachesPublicApiCallbacks() {
        AtomicInteger calls = new AtomicInteger();
        BaniraClientEvents.Client.onClientTick(event -> calls.incrementAndGet());

        BaniraClientEventHub.dispatchClientTick(BaniraClientTickEvent.END);

        assertEquals(1, calls.get());
    }
}
