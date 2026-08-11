package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 未被控件消费的关闭快捷键必须统一经过未保存检查。 */
public class NotificationTypeConfigEscapeContractTest {
    @Test
    public void closeShortcutsUseTheUnsavedChangesGuard() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/NotificationTypeConfigScreen.java")),
                StandardCharsets.UTF_8);

        int override = source.indexOf("protected boolean requestClose(CloseReason reason)");
        int changes = source.indexOf("changedSettingCount()", override);
        int warning = source.indexOf("config_editor_unsaved_changes", override);
        assertTrue("Notification type config must guard every generic close request", override >= 0);
        assertTrue("The close guard must check pending changes", changes > override);
        assertTrue("The close guard must explain why closing was blocked", warning > changes);
    }
}
