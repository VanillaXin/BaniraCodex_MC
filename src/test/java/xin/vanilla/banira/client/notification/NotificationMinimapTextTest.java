package xin.vanilla.banira.client.notification;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import static org.junit.Assert.*;
public class NotificationMinimapTextTest {
    private JsonObject language(String code) throws Exception {
        try(InputStream input=getClass().getResourceAsStream("/assets/banira_codex/lang/"+code+".json")){
            assertNotNull(input);return new Gson().fromJson(new InputStreamReader(input,StandardCharsets.UTF_8),JsonObject.class);
        }
    }
    @Test public void englishUsesCompactUnreadCountWithoutChangingHistory() throws Exception {
        JsonObject lang=language("en_us");
        assertNotNull(lang.get("format.banira_codex.notification_minimap_unread"));
        assertEquals("23 unread",String.format(Locale.ROOT,lang.get("format.banira_codex.notification_minimap_unread").getAsString(),23));
        assertEquals("No unread notifications",lang.get("word.banira_codex.notification_minimap_empty").getAsString());
        assertEquals("Unread",lang.get("word.banira_codex.notification_log_unread").getAsString());
    }
    @Test public void chineseUsesCountAndExplicitEmptyState() throws Exception {
        JsonObject lang=language("zh_cn");
        assertNotNull(lang.get("format.banira_codex.notification_minimap_unread"));
        assertEquals("\u0032\u0033\u6761\u672a\u8bfb\u901a\u77e5",String.format(Locale.ROOT,lang.get("format.banira_codex.notification_minimap_unread").getAsString(),23));
        assertEquals("\u6682\u65e0\u672a\u8bfb\u901a\u77e5",lang.get("word.banira_codex.notification_minimap_empty").getAsString());
    }
    @Test public void mapTextUsesCountAndEmptyTranslationKeys() throws Exception {
        ClassNode node=new ClassNode();
        try(InputStream in=getClass().getResourceAsStream("/xin/vanilla/banira/client/notification/NotificationMinimapBridge.class")){
            assertNotNull(in);new ClassReader(in).accept(node,0);
        }
        boolean count=false,empty=false;
        for(MethodNode m:node.methods)if(m.name.equals("text"))for(AbstractInsnNode i:m.instructions)if(i instanceof LdcInsnNode){
            count|="notification_minimap_unread".equals(((LdcInsnNode)i).cst);empty|="notification_minimap_empty".equals(((LdcInsnNode)i).cst);
        }
        assertTrue(count && empty);
    }
}
