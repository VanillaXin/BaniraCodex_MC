package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 锁定通知在界面快捷入口之后绘制，避免被入口图标遮挡。 */
public class NotificationLayerOrderContractTest {
    @Test
    public void screenNotificationRendersAfterQuickActions() throws IOException {
        Path path = Paths.get("src/main/java/xin/vanilla/banira/internal/client/BaniraClientEventBridge.java");
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        int method = source.indexOf("fireDrawScreenPost");
        int quickAction = source.indexOf("QuickActionOverlay.get().render", method);
        int notification = source.indexOf("NotificationManager.get().render", method);

        assertTrue(quickAction >= 0);
        assertTrue(notification > quickAction);
    }
}
