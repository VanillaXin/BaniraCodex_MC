package xin.vanilla.banira.api.event;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaniraLifecycleTest {

    @Test
    public void commonSetupUsesLoaderNeutralEventAndRegistration() {
        List<String> calls = new ArrayList<>();

        BaniraEventRegistration registration = BaniraLifecycle.onCommonSetup(event ->
                event.enqueueWork(() -> calls.add("queued"))
        );

        BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());
        registration.unregister();
        BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());

        assertEquals(Collections.singletonList("queued"), calls);
    }
}
