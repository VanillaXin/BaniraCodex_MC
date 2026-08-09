package xin.vanilla.banira.api;

import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BaniraModPresenceTest {

    @After
    public void clearRegistrations() {
        for (String modId : List.copyOf(BaniraModPresence.announcedModIds())) {
            BaniraModPresence.unregister(modId);
        }
    }

    @Test
    public void registersAndUnregistersModPresence() {
        BaniraModPresenceRegistration registration = BaniraModPresence.register(" child_mod ");

        assertTrue(BaniraModPresence.hasRegistration("child_mod"));
        assertEquals(List.of("child_mod"), BaniraModPresence.announcedModIds());

        registration.close();

        assertFalse(BaniraModPresence.hasRegistration("child_mod"));
    }

    @Test
    public void emptyRegistrationIsNoop() {
        BaniraModPresenceRegistration registration = BaniraModPresence.register("   ");

        registration.close();

        assertTrue(BaniraModPresence.announcedModIds().isEmpty());
    }
}
