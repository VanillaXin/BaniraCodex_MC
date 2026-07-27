package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * 防止通知类型管理页再次退回原始 id，或保存后没有任何反馈。
 */
public class NotificationTypeConfigMetadataContractTest {

    @Test
    public void screenConsumesRegisteredMetadataAndReportsSaveSuccess() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/client/gui/NotificationTypeConfigScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("NotificationTypeRegistry.modDisplayName(segment)"));
        assertTrue(source.contains("NotificationTypeRegistry.tooltip(typeId)"));
        assertTrue(source.contains("NotificationTypeKeys.DEFAULT.equals(typeId)"));
        assertTrue(source.contains("ConfigEditorNotifier.show(\"config_editor_save_success\", 2000)"));
    }
}
