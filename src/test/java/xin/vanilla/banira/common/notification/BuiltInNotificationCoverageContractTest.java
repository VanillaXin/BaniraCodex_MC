package xin.vanilla.banira.common.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BuiltInNotificationCoverageContractTest {

    @Test
    public void builtInPlayerMessagesUseRegisteredNotificationTypes() throws Exception {
        assertTrue(ServerNotificationTypeRegistry.sortedSnapshot().contains(NotificationTypeKeys.HELP));
        assertTrue(ServerNotificationTypeRegistry.sortedSnapshot().contains(NotificationTypeKeys.COMMAND_FEEDBACK));
        assertEquals(EnumNotificationTypeDisplayMode.VANILLA_CHAT, displayDefault(NotificationTypeKeys.HELP));
        assertEquals(EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                displayDefault(NotificationTypeKeys.COMMAND_FEEDBACK));

    }

    private EnumNotificationTypeDisplayMode displayDefault(String typeId) {
        return ServerNotificationTypeRegistry.buildSyncEntries().stream()
                .filter(entry -> typeId.equals(entry.typeId()))
                .findFirst()
                .orElseThrow(IllegalStateException::new)
                .defaultDisplayIfAbsent();
    }
}
