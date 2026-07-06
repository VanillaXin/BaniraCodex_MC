package xin.vanilla.banira.api.client.notification;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.platform.BaniraNotificationService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.*;

public class BaniraNotificationsTest {

    @Test
    public void forwardsComponentToPlatformService() {
        RecordingNotificationService service = new RecordingNotificationService();
        BaniraPlatforms.install(new TestBaniraPlatform().client(true).notificationService(service));

        Component component = BaniraComponent.get().literal("hello");
        BaniraNotifications.show(component);

        assertSame(component, service.component);
        assertNull(service.notification);
    }

    @Test
    public void forwardsNotificationDataToPlatformService() {
        RecordingNotificationService service = new RecordingNotificationService();
        BaniraPlatforms.install(new TestBaniraPlatform().client(true).notificationService(service));

        NotificationData data = NotificationData.of(BaniraComponent.get().literal("hello"), null, null, 1000L);
        BaniraNotifications.show(data);

        assertSame(data, service.notification);
        assertFalse(service.fromNetwork);
    }

    private static final class RecordingNotificationService implements BaniraNotificationService {
        private Component component;
        private NotificationData notification;
        private boolean fromNetwork;

        @Override
        public void show(Component component) {
            this.component = component;
        }

        @Override
        public void show(NotificationData notification) {
            this.notification = notification;
        }

        @Override
        public void show(NotificationData notification, boolean fromNetwork) {
            this.notification = notification;
            this.fromNetwork = fromNetwork;
        }
    }
}
