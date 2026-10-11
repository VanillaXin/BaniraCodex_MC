package xin.vanilla.banira.client.notification;
import org.junit.Test;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.common.enums.EnumPosition;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.Assert.*;

public class NotificationTypedMuteManagerTest {
    private Object field(Object object,String name) throws Exception {
        Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    private Notification item(String type) throws Exception {
        Class<?> unsafe=Class.forName("sun.misc.Unsafe");
        Field f=unsafe.getDeclaredField("theUnsafe");f.setAccessible(true);
        Notification n=(Notification)unsafe.getMethod("allocateInstance",Class.class).invoke(f.get(null),Notification.class);
        n.notificationType(type);
        Field a=Notification.class.getDeclaredField("animationState");a.setAccessible(true);java.lang.reflect.Constructor<?> c=a.getType().getDeclaredConstructor();c.setAccessible(true);a.set(n,c.newInstance());
        return n;
    }
    private void mute(NotificationManager manager,String type) throws Exception {
        Method method=null;
        for(Method m:NotificationManager.class.getDeclaredMethods())if(m.getName().equals("muteType"))method=m;
        assertNotNull("Manager type mute",method);method.invoke(manager,type);
    }
    @SuppressWarnings("unchecked")
    @Test public void removesSelectedTypeAcrossRegionsWithoutClearingOtherHitTargets() throws Exception {
        NotificationManager manager=new NotificationManager();
        Map<EnumPosition,List<Notification>> queues=(Map<EnumPosition,List<Notification>>)field(manager,"notifications");
        Notification a=item("test.a"),alias=item(" test.a "),b=item("test.b");
        queues.put(EnumPosition.TOP_LEFT,new ArrayList<>(Arrays.asList(a,b)));
        queues.put(EnumPosition.BOTTOM_RIGHT,new ArrayList<>(Collections.singletonList(alias)));
        List<Notification> frame=(List<Notification>)field(manager,"frameDrawOrder");frame.addAll(Arrays.asList(a,alias,b));
        mute(manager,"test.a");
        assertTrue(a.finished());assertTrue(alias.finished());assertFalse(b.finished());
        assertEquals(Collections.singletonList(b),frame);
    }
    @Test public void mutingDoesNotReadOrDeleteHistory() throws Exception {
        NotificationManager manager=new NotificationManager();
        NotificationHistory history=(NotificationHistory)field(manager,"history");
        history.append(new NotificationLogEntry().id(1).notificationType("test.a"),100);
        history.append(new NotificationLogEntry().id(2).notificationType("test.b"),100);
        long revision=manager.historyRevision();
        mute(manager,"test.a");
        assertEquals(2,manager.unreadCount());assertEquals(2,manager.getLog().size());
        assertEquals(revision,manager.historyRevision());
    }

    @Test public void typedGuardSkipsVanillaDeliveryButPreservesHistoryOrder() throws Exception {
        org.objectweb.asm.tree.ClassNode node=new org.objectweb.asm.tree.ClassNode();
        try(java.io.InputStream in=getClass().getResourceAsStream("/xin/vanilla/banira/client/util/NotificationManager.class")){
            assertNotNull(in);new org.objectweb.asm.ClassReader(in).accept(node,0);
        }
        for(org.objectweb.asm.tree.MethodNode m:node.methods){
            if(!m.name.equals("addNotification") || !m.desc.endsWith("Z)V"))continue;
            int append=-1,typed=-1,vanilla=-1,legacy=-1;org.objectweb.asm.tree.JumpInsnNode skip=null;
            for(int i=0;i<m.instructions.size();i++){
                org.objectweb.asm.tree.AbstractInsnNode ins=m.instructions.get(i);
                if(!(ins instanceof org.objectweb.asm.tree.MethodInsnNode))continue;
                org.objectweb.asm.tree.MethodInsnNode call=(org.objectweb.asm.tree.MethodInsnNode)ins;
                if(call.name.equals("appendLog"))append=i;
                if(call.name.equals("deliverVanillaIfConfigured"))vanilla=i;
                if(call.name.equals("muted") && call.desc.equals("()Z"))legacy=i;
                if(call.name.equals("muted") && call.desc.equals("(Ljava/lang/String;)Z")){
                    typed=i;org.objectweb.asm.tree.AbstractInsnNode next=ins.getNext();
                    while(next!=null && next.getOpcode()<0)next=next.getNext();
                    if(next instanceof org.objectweb.asm.tree.JumpInsnNode)skip=(org.objectweb.asm.tree.JumpInsnNode)next;
                }
            }
            assertTrue("History before typed suppression",append>=0 && typed>append);
            assertTrue("Typed suppression before vanilla delivery",vanilla>typed);
            assertTrue("Legacy global mute remains overlay-only",legacy>vanilla);
            assertNotNull("Muted result must skip delivery",skip);
            assertEquals(org.objectweb.asm.Opcodes.IFNE,skip.getOpcode());
            assertTrue(m.instructions.indexOf(skip.label)>vanilla);
            return;
        }
        fail("Notification arrival method missing");
    }
    @SuppressWarnings("unchecked")
    @Test public void rebuiltFadingHitTargetsCannotConsumeCloseOrBodyClicks() throws Exception {
        NotificationManager manager=new NotificationManager();
        Notification a=item("test.a"),pending=item("test.a");
        a.animationTime(600);
        Object animation=field(a,"animationState");
        Method start=animation.getClass().getDeclaredMethod("start",long.class,long.class,long.class);
        start.setAccessible(true);start.invoke(animation,System.currentTimeMillis()-1000,600L,120000L);
        Map<EnumPosition,List<Notification>> queues=(Map<EnumPosition,List<Notification>>)field(manager,"notifications");
        queues.put(EnumPosition.TOP_LEFT,new ArrayList<>(Arrays.asList(a,pending)));
        mute(manager,"test.a");
        assertFalse("Started animation must exit rather than finish instantly",a.finished());
        assertTrue(pending.finished());
        for(Map.Entry<String,Double> e:new LinkedHashMap<String,Double>(){{
            put("hitX",0.0);put("hitY",0.0);put("hitW",200.0);put("hitH",40.0);
            put("closeBtnLeft",180.0);put("closeBtnTop",0.0);put("closeBtnSize",11.0);
            put("bodyLeft",0.0);put("bodyTop",0.0);put("bodyW",170.0);put("bodyH",40.0);
        }}.entrySet()){Field f=Notification.class.getDeclaredField(e.getKey());f.setAccessible(true);f.setDouble(a,e.getValue());}
        List<Notification> frame=(List<Notification>)field(manager,"frameDrawOrder");frame.add(a);
        assertTrue(a.containsPoint(185,5));assertTrue(a.isCloseHit(185,5));
        assertFalse(manager.tryHandleHudClick(185,5,0));
        assertTrue(a.isBodyHit(10,10));assertFalse(manager.tryHandleHudClick(10,10,0));
    }

    @Test public void typedTooltipUsesFormatLanguageNamespace() throws Exception {
        String[] langs={"en_us","zh_cn"};
        String[] expected={"Mute this notification type for 10 minutes","屏蔽此类型通知 10 分钟"};
        for(int i=0;i<langs.length;i++){
            try(java.io.InputStream stream=getClass().getResourceAsStream("/assets/banira_codex/lang/"+langs[i]+".json")){
                assertNotNull(stream);
                com.google.gson.JsonObject json=new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                com.google.gson.JsonElement template=json.get("format.banira_codex.notification_mute_type_minutes");
                assertNotNull("Parameterized tooltip must use FORMAT resources",template);
                assertEquals(expected[i],xin.vanilla.banira.common.util.StringUtils.format(template.getAsString(),"10"));
            }
        }
    }
}
