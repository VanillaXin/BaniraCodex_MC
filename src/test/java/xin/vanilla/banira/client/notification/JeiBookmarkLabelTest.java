package xin.vanilla.banira.client.notification;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class JeiBookmarkLabelTest {
    @Test public void bookmarkNameUsesStableOwnedTranslation() throws Exception {
        ClassNode node=new ClassNode();
        try(InputStream in=getClass().getResourceAsStream("/xin/vanilla/banira/internal/neoforge/compat/jei/JeiCompatibility$BookmarkProvider.class")){
            assertNotNull(in);new ClassReader(in).accept(node,0);
        }
        boolean own=false;
        for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof LdcInsnNode){
            Object value=((LdcInsnNode)i).cst;
            assertNotEquals("JEI removed this key in21", "jei.tooltip.bookmarks",value);
            own|="word.banira_codex.jei_bookmarks".equals(value);
        }
        assertTrue(own);
        for(String code:new String[]{"en_us","zh_cn"})try(InputStream in=getClass().getResourceAsStream("/assets/banira_codex/lang/"+code+".json")){
            JsonObject language=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),JsonObject.class);
            assertTrue(language.has("word.banira_codex.jei_bookmarks"));
        }
    }
}
