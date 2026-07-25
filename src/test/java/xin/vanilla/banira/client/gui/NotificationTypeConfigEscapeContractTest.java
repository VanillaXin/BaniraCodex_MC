package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定 ESC 在子控件之前处理，避免折叠面板吞掉关闭命令。 */
public class NotificationTypeConfigEscapeContractTest {
    @Test
    public void escapeIsHandledBeforeDelegatingToChildWidgets() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/NotificationTypeConfigScreen.java")),
                StandardCharsets.UTF_8);

        int override = source.indexOf("public boolean keyPressed(int keyCode, int scanCode, int modifiers)");
        int escape = source.indexOf("keyCode != GLFWKey.GLFW_KEY_ESCAPE", override);
        int delegate = source.indexOf("super.keyPressed(keyCode, scanCode, modifiers)", override);
        assertTrue("Notification type config must override the key dispatch entry point", override >= 0);
        assertTrue("ESC must be checked before child-widget delegation", escape > override && delegate > escape);
        assertFalse("Late onKeyPressed handling lets child widgets consume ESC first",
                source.contains("protected void onKeyPressed(KeyPressedHandleArgs"));
    }
}
