package xin.vanilla.banira.client.notification;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.InputStream;
import static org.junit.Assert.*;
public class JourneyMapPermissionTest {
    private ClassNode read() throws Exception {
        ClassNode node=new ClassNode();
        try(InputStream in=getClass().getResourceAsStream("/xin/vanilla/banira/internal/forge/compat/minimap/JourneyMapNotificationPlugin.class")) {
            assertNotNull(in);new ClassReader(in).accept(node,0);
        }
        return node;
    }
    @Test public void supplierChecksNativePermissionBeforeClaimingHud() throws Exception {
        ClassNode node=read();boolean guarded=false;
        for(MethodNode m:node.methods){boolean permission=false;
            for(AbstractInsnNode i:m.instructions) if(i instanceof MethodInsnNode){
                MethodInsnNode call=(MethodInsnNode)i;
                if(call.owner.equals("journeymap/client/InternalStateHandler") && call.name.equals("isMinimapEnabled"))permission=true;
                if(call.owner.endsWith("/NotificationMinimapBridge") && call.name.equals("text")){assertTrue("Disabled native minimap must not claim HUD",permission);guarded=true;}
            }
        }
        assertTrue(guarded);
    }
    @Test public void permissionTransitionIsTrackedForLayoutRefresh() throws Exception {
        ClassNode node=read();
        assertTrue("Restoring permission must rebuild previously empty slots",node.fields.stream().anyMatch(f -> f.name.equals("previousAllowed") && f.desc.equals("Z")));
        MethodNode refresh=node.methods.stream().filter(m -> m.name.equals("refreshLayout")).findFirst().get();
        boolean read=false,write=false;
        for(AbstractInsnNode i:refresh.instructions) if(i instanceof FieldInsnNode){
            FieldInsnNode field=(FieldInsnNode)i;if(field.name.equals("previousAllowed")){read|=field.getOpcode()==180;write|=field.getOpcode()==181;}
        }
        assertTrue(read && write);
    }
}
