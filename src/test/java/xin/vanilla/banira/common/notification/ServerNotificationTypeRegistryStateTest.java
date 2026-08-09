package xin.vanilla.banira.common.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;

import java.util.List;

import static org.junit.Assert.*;

public class ServerNotificationTypeRegistryStateTest {

    @Test
    public void startsWithDefaultType() {
        ServerNotificationTypeRegistryState state = new ServerNotificationTypeRegistryState();

        assertEquals(List.of(NotificationTypeKeys.DEFAULT), state.sortedSnapshot());
        assertEquals(EnumPosition.TOP_RIGHT, state.defaultLayout("missing").position());
        assertEquals(EnumMoveType.AUTO, state.defaultLayout("missing").animation());
    }

    @Test
    public void registerKeepsSortedSnapshotAndNormalizesTypeId() {
        ServerNotificationTypeRegistryState state = new ServerNotificationTypeRegistryState();

        state.register(" zeta ");
        state.register("alpha");

        assertEquals(List.of("alpha", NotificationTypeKeys.DEFAULT, "zeta"), state.sortedSnapshot());
    }

    @Test
    public void layoutRegistrationDefaultsNullArguments() {
        ServerNotificationTypeRegistryState state = new ServerNotificationTypeRegistryState();

        state.register("countdown", null, EnumMoveType.FADE_IN);

        assertEquals(EnumPosition.TOP_RIGHT, state.defaultLayout("countdown").position());
        assertEquals(EnumMoveType.FADE_IN, state.defaultLayout("countdown").animation());
    }

    @Test
    public void clientDisplayDefaultIsIncludedInSyncEntries() {
        ServerNotificationTypeRegistryState state = new ServerNotificationTypeRegistryState();

        state.register("countdown", EnumNotificationTypeDisplayMode.ACTION_BAR);

        List<NotificationTypeSyncEntry> entries = state.buildSyncEntries();

        NotificationTypeSyncEntry countdown = entries.stream()
                .filter(e -> "countdown".equals(e.typeId()))
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals(EnumNotificationTypeDisplayMode.ACTION_BAR, countdown.defaultDisplayIfAbsent());
    }

    @Test
    public void registerWithoutDisplayDoesNotClearPreviousDisplayDefault() {
        ServerNotificationTypeRegistryState state = new ServerNotificationTypeRegistryState();

        state.register("countdown", EnumNotificationTypeDisplayMode.ACTION_BAR);
        state.register("countdown");

        assertEquals(EnumNotificationTypeDisplayMode.ACTION_BAR, state.clientDisplayIfAbsent("countdown"));
    }

    @Test
    public void unregisterRemovesCustomTypeButKeepsDefault() {
        ServerNotificationTypeRegistryState state = new ServerNotificationTypeRegistryState();
        state.register("countdown", EnumPosition.BOTTOM_CENTER, EnumMoveType.FADE_IN,
                EnumNotificationTypeDisplayMode.ACTION_BAR);

        assertTrue(state.unregister("countdown"));
        assertFalse(state.unregister(NotificationTypeKeys.DEFAULT));
        assertNull(state.clientDisplayIfAbsent("countdown"));
        assertEquals(List.of(NotificationTypeKeys.DEFAULT), state.sortedSnapshot());
    }
}
