package xin.vanilla.banira.api.event;

import org.junit.Test;
import xin.vanilla.banira.common.util.BaniraEventBus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaniraLifecycleTest {

    @Test
    public void commonSetupUsesLoaderNeutralEventAndRegistration() {
        List<String> calls = new ArrayList<>();

        BaniraEventBus.Registration registration = BaniraLifecycle.onCommonSetup(event ->
                event.enqueueWork(() -> calls.add("queued"))
        );

        BaniraEventBus.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());
        registration.unregister();
        BaniraEventBus.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());

        assertEquals(List.of("queued"), calls);
    }
}
