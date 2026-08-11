package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClientNotificationTypeRegistryStateTest {

    @Test
    public void startsWithDefaultType() {
        ClientNotificationTypeRegistryState state = new ClientNotificationTypeRegistryState();

        assertEquals(List.of(NotificationTypeKeys.DEFAULT), state.knownTypesSorted(Set.of()));
    }

    @Test
    public void knownTypesIncludeStoredAndServerTypesSorted() {
        ClientNotificationTypeRegistryState state = new ClientNotificationTypeRegistryState();

        state.register("zeta");
        state.registerAllFromServer(Arrays.asList("alpha", null, " beta "));

        assertEquals(List.of("alpha", "beta", NotificationTypeKeys.DEFAULT, "stored", "zeta"),
                state.knownTypesSorted(Set.of("stored")));
    }

    @Test
    public void modRegisteredDisplayDefaultWinsOverServerSyncedDefault() {
        ClientNotificationTypeRegistryState state = new ClientNotificationTypeRegistryState();

        state.register("countdown", EnumNotificationTypeDisplayMode.ACTION_BAR);
        state.acceptServerSyncedDisplayDefault("countdown", EnumNotificationTypeDisplayMode.VANILLA_CHAT);

        assertEquals(EnumNotificationTypeDisplayMode.ACTION_BAR, state.resolvedDisplayDefault("countdown"));
    }

    @Test
    public void removingModDefaultRevealsServerSyncedDefault() {
        ClientNotificationTypeRegistryState state = new ClientNotificationTypeRegistryState();

        state.acceptServerSyncedDisplayDefault("countdown", EnumNotificationTypeDisplayMode.VANILLA_CHAT);
        state.register("countdown", EnumNotificationTypeDisplayMode.ACTION_BAR);
        state.register("countdown", null);

        assertEquals(EnumNotificationTypeDisplayMode.VANILLA_CHAT, state.resolvedDisplayDefault("countdown"));
    }

    @Test
    public void resolvedDefaultIdsContainBothSources() {
        ClientNotificationTypeRegistryState state = new ClientNotificationTypeRegistryState();

        state.register("local", EnumNotificationTypeDisplayMode.ACTION_BAR);
        state.acceptServerSyncedDisplayDefault("server", EnumNotificationTypeDisplayMode.VANILLA_CHAT);

        assertEquals(Set.of("local", "server"), state.typeIdsWithResolvedDefaults());
    }

    @Test
    public void clearRestoresDefaultOnly() {
        ClientNotificationTypeRegistryState state = new ClientNotificationTypeRegistryState();
        state.register("countdown", EnumNotificationTypeDisplayMode.ACTION_BAR);

        state.clear();

        assertEquals(List.of(NotificationTypeKeys.DEFAULT), state.knownTypesSorted(Set.of()));
        assertNull(state.resolvedDisplayDefault("countdown"));
    }
}
