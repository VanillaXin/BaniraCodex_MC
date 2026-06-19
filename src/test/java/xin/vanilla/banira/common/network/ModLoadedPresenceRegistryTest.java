package xin.vanilla.banira.common.network;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ModLoadedPresenceRegistryTest {

    @Test
    public void registerKeepsOrderAndAllowsUnregister() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();

        ModLoadedRegistration alpha = registry.register(" alpha ", player -> {
        });
        registry.register("beta", player -> {
        });

        assertEquals(Arrays.asList("alpha", "beta"), registry.announcedModIds());
        assertTrue(registry.hasRegistration("alpha"));

        alpha.close();

        assertEquals(Collections.singletonList("beta"), registry.announcedModIds());
        assertFalse(registry.hasRegistration("alpha"));
    }

    @Test
    public void blankRegistrationIsNoop() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();

        ModLoadedRegistration registration = registry.register("   ", player -> {
        });
        registration.close();

        assertTrue(registry.announcedModIds().isEmpty());
        assertFalse(registry.unregister("   "));
    }

    @Test
    public void closeOnlyRemovesTheSameCallback() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();
        AtomicInteger calls = new AtomicInteger();

        ModLoadedRegistration first = registry.register("demo", player -> calls.addAndGet(1));
        registry.register("demo", player -> calls.addAndGet(10));

        first.close();

        assertTrue(registry.hasRegistration("demo"));
    }
}
