package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 锁定实时通知与通知记录共用的保色对比度处理。 */
public class NotificationColorPreservationContractTest {
    @Test
    public void notificationAndLogRenderReadableRgbComponents() throws Exception {
        String notification = read("src/main/java/xin/vanilla/banira/client/gui/component/Notification.java");
        String logScreen = read("src/main/java/xin/vanilla/banira/client/gui/NotificationLogScreen.java");

        assertTrue(notification.contains("ColorUtils.readableVanillaComponentCopy"));
        assertTrue(notification.contains("this.component().toVanilla(lang)"));
        assertTrue(notification.contains("c.color().isEmpty() || c.color().rgb() == 0xFFFFFF"));
        assertTrue(notification.contains("fromNetwork || data.themed()"));
        assertTrue(notification.contains("n.themed(themed)"));
        assertTrue(notification.contains("separator = type.indexOf('.')"));
        assertTrue(notification.contains("BaniraThemes.seasonFor(type.substring(0, separator))"));
        assertTrue(!notification.contains("richNeedsContrastShadow"));
        assertTrue(logScreen.contains("ColorUtils.readableVanillaComponentCopy("));
        assertTrue(logScreen.contains("entry.component().toVanilla(language)"));
        assertTrue(!logScreen.contains("detailNeedsContrastShadow"));
        assertTrue(!logScreen.contains("font.drawShadow(stack, line"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
