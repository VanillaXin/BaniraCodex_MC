package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/** 验证通知类型配置草稿不会直接修改存储对象。 */
public class NotificationTypeSettingsStoreTest {
    @Test
    public void copyOfCreatesIndependentSettings() {
        NotificationTypeSettingsStore.TypeSettings source = new NotificationTypeSettingsStore.TypeSettings()
                .hidden(true)
                .durationMs(2500)
                .positionName("TOP_RIGHT")
                .animationName("FADE_IN")
                .displayMode(EnumNotificationTypeDisplayMode.OVERLAY);

        NotificationTypeSettingsStore.TypeSettings copy = NotificationTypeSettingsStore.copyOf(source);
        copy.durationMs(5000);

        assertNotSame(source, copy);
        assertEquals(2500, source.durationMs());
        assertEquals(5000, copy.durationMs());
    }
}
