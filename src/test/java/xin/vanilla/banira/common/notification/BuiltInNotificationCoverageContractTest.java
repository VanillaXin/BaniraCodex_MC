package xin.vanilla.banira.common.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

        String help = source("src/main/java/xin/vanilla/banira/command/impl/HelpCommand.java");
        assertTrue(help.contains("MessageUtils.sendNotification(player, helpInfo, NotificationTypeKeys.HELP)"));
    }

    private EnumNotificationTypeDisplayMode displayDefault(String typeId) {
        return ServerNotificationTypeRegistry.buildSyncEntries().stream()
                .filter(entry -> typeId.equals(entry.typeId()))
                .findFirst()
                .orElseThrow()
                .defaultDisplayIfAbsent();
    }

    private String source(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}
