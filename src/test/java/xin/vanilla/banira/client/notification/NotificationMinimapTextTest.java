package xin.vanilla.banira.client.notification;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class NotificationMinimapTextTest {
    private JsonObject language(String code) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/assets/banira_codex/lang/" + code + ".json")) {
            assertNotNull(input);
            return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
        }
    }

    @Test public void englishUsesNotificationsWithoutChangingUnreadHistory() throws Exception {
        JsonObject lang = language("en_us");
        assertNotNull("Minimap label missing", lang.get("word.banira_codex.notification_minimap_label"));
        assertEquals("Notifications", lang.get("word.banira_codex.notification_minimap_label").getAsString());
        assertEquals("Unread", lang.get("word.banira_codex.notification_log_unread").getAsString());
    }

    @Test public void chineseUsesNotificationsWithoutChangingUnreadHistory() throws Exception {
        JsonObject lang = language("zh_cn");
        assertEquals("\u901a\u77e5", lang.get("word.banira_codex.notification_minimap_label").getAsString());
        assertEquals("\u672a\u8bfb", lang.get("word.banira_codex.notification_log_unread").getAsString());
    }

    @Test public void mapTextUsesDedicatedTranslation() throws Exception {
        ClassNode node = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/client/notification/NotificationMinimapBridge.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(node, 0);
        }
        boolean dedicated = false;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("text")) continue;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof LdcInsnNode)) continue;
                Object value = ((LdcInsnNode) instruction).cst;
                assertNotEquals("History's unread label must stay separate", "notification_log_unread", value);
                dedicated |= "notification_minimap_label".equals(value);
            }
        }
        assertTrue(dedicated);
    }
}
