package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * 锁定子 Mod 登记的通知类型说明与本地化名称不会暴露可变内部对象。
 */
public class NotificationTypeRegistryMetadataTest {

    @Test
    public void registeredMetadataIsDefensivelyCopied() {
        String typeId = "metadata_test.teleport";
        Component tooltip = BaniraComponent.get().literal("Teleport request");
        NotificationTypeRegistry.register(typeId, tooltip);
        tooltip.text("changed");

        Component firstTooltip = NotificationTypeRegistry.tooltip(typeId);
        Component secondTooltip = NotificationTypeRegistry.tooltip(typeId);
        assertEquals("Teleport request", firstTooltip.toString());
        assertEquals("Teleport request", secondTooltip.toString());
        assertNotSame(firstTooltip, secondTooltip);

        Component displayName = BaniraComponent.get().literal("Metadata Test");
        NotificationTypeRegistry.registerModDisplayName("metadata_test", displayName);
        displayName.text("changed");

        Component firstName = NotificationTypeRegistry.modDisplayName("metadata_test");
        Component secondName = NotificationTypeRegistry.modDisplayName("METADATA_TEST");
        assertEquals("Metadata Test", firstName.toString());
        assertEquals("Metadata Test", secondName.toString());
        assertNotSame(firstName, secondName);
    }
}
