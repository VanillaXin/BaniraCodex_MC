package xin.vanilla.banira.common.network;

import net.minecraft.server.level.ServerPlayer;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class ModLoadedPresenceRegistryTest {

    @Test
    public void registerKeepsDeclaredOrderAndTrimsModId() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();

        registry.register(" banira_codex ", player -> {
        });
        registry.register("child_mod", player -> {
        });

        assertEquals(List.of("banira_codex", "child_mod"), registry.announcedModIds());
    }

    @Test
    public void emptyModIdRegistrationIsIgnored() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();

        registry.register("   ", player -> {
        });

        assertTrue(registry.announcedModIds().isEmpty());
        assertFalse(registry.hasRegistration("   "));
    }

    @Test
    public void registrationTokenRemovesCurrentDeclaration() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();
        ModLoadedRegistration registration = registry.register("banira_codex", player -> {
        });

        assertTrue(registry.hasRegistration("banira_codex"));

        registration.close();

        assertFalse(registry.hasRegistration("banira_codex"));
    }

    @Test
    public void oldRegistrationTokenDoesNotRemoveReplacement() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();
        Consumer<ServerPlayer> oldHandler = player -> {
        };
        Consumer<ServerPlayer> newHandler = player -> {
        };
        ModLoadedRegistration oldRegistration = registry.register("banira_codex", oldHandler);
        ModLoadedRegistration newRegistration = registry.register("banira_codex", newHandler);

        oldRegistration.close();

        assertTrue(registry.hasRegistration("banira_codex"));

        newRegistration.close();

        assertFalse(registry.hasRegistration("banira_codex"));
    }

    @Test
    public void dispatchWithoutPlayerIsIgnored() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();
        registry.register("banira_codex", player -> {
            throw new AssertionError("handler must not run without a server player");
        });

        assertFalse(registry.dispatchServerSync(null, "banira_codex"));
    }

    @Test
    public void clearRemovesAllDeclarations() {
        ModLoadedPresenceRegistry registry = new ModLoadedPresenceRegistry();
        registry.register("banira_codex", player -> {
        });
        registry.register("child_mod", player -> {
        });

        registry.clear();

        assertTrue(registry.announcedModIds().isEmpty());
    }
}
