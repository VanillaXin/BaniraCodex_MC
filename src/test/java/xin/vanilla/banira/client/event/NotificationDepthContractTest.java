package xin.vanilla.banira.client.event;

import org.junit.Test;
import xin.vanilla.banira.client.enums.EnumRenderDepth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定快捷入口低于通知，避免调用顺序正确但仍被深度测试遮挡。 */
public class NotificationDepthContractTest {
    @Test
    public void quickActionTrayUsesNamedOverlayDepthBelowNotifications() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/quickaction/QuickActionOverlay.java")),
                StandardCharsets.UTF_8);

        assertTrue(EnumRenderDepth.OVERLAY.depth() < EnumRenderDepth.NOTIFICATION.depth());
        assertTrue(source.contains("EnumRenderDepth.OVERLAY.depth()"));
        assertFalse(source.contains("translate(0, 0, 800)"));
        assertFalse(source.contains("translate(0, 0, 4000)"));
    }
}
