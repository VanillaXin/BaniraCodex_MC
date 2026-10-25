package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.internal.client.ui.page.NotificationUiSession;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiHudPositionPageTest {
    @org.junit.BeforeClass public static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @org.junit.Before public void platform(){
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
        xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().registerIcon("banira_codex:b06_header_test",
                xin.vanilla.banira.client.gui.quickaction.QuickIcon.resource(xin.vanilla.banira.client.data.Texture.of(
                        new net.minecraft.resources.ResourceLocation("banira_codex","gui/quick_icon.png"),64,64)),
                xin.vanilla.banira.BaniraComponent.get().literal("HUD position"),null);
    }
    @org.junit.After public void releaseIcon(){xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().unregister("banira_codex:b06_header_test");}
    private String resource(String path) throws Exception {
        try(var in=getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/"+path)) {
            assertNotNull(path,in);return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    @Test public void fullViewportRetainsPreviewAndCommonHeaderAndFooter() throws Exception {
        String html=resource("ui/hud-position.html");
        for(String id:new String[]{"viewport","preview","toolbar","brand","title","module_name","origin","close","status","error","cancel","save"})assertTrue(id,html.contains("id=\""+id+"\""));
        assertTrue(html.contains("href=\"config.css\""));assertTrue(html.contains("href=\"player-config-editor.css\""));
        assertTrue(html.contains("class=\"header config-header\""));assertTrue(html.contains("class=\"footer config-footer\""));
        assertTrue(html.contains("<body id=\"viewport\""));assertFalse(html.contains("<script"));
        String css=resource("shared.css");assertTrue(css.contains(".hud-position #toolbar"));
        assertFalse(css.matches("(?s).*\\.hud-position \\.footer\\s*\\{[^}]*position:\\s*fixed.*"));
    }
    @Test public void commonHeaderAndCancelRetainDraftAndReleaseOldActions() throws Exception {
        var f=new UiNotificationPagesTest.Fixture();var session=(NotificationUiSession)f.session("PositionPort");
        var old=new NativeUiView(new Object());session.bind(old);
        assertNotNull(old.gameElements.get("brand"));assertNotNull(old.texts.get("module_name"));assertNotNull(old.texts.get("origin"));
        session.moveTo(.6,.2);assertEquals(0,f.saves);assertNotNull(old.texts.get("status"));
        assertFalse(old.texts.get("status").isEmpty());
        var current=new NativeUiView(new Object());var closed=new java.util.concurrent.atomic.AtomicInteger();
        current.onClose(closed::incrementAndGet);session.bind(current);
        old.activate("cancel");old.activate("save");assertEquals(0,closed.get());assertEquals(0,f.saves);
        current.activate("cancel");assertEquals(1,closed.get());assertEquals(0,f.saves);
        assertEquals(.25,f.x,0);assertEquals(.75,f.y,0);
    }
    @Test public void rejectedSaveKeepsDirtyDraftAndShowsActualError() throws Exception {
        var f=new UiNotificationPagesTest.Fixture();f.failSave=true;
        var session=(NotificationUiSession)f.session("PositionPort");var view=new NativeUiView(new Object());
        var closed=new java.util.concurrent.atomic.AtomicInteger();view.onClose(closed::incrementAndGet);session.bind(view);
        session.moveTo(.8,.1);view.activate("save");
        assertEquals(0,closed.get());assertEquals(0,f.saves);assertEquals(1,session.changedCount());
        assertEquals(.8,session.x(),0);assertEquals(.1,session.y(),0);
        assertNotNull(view.texts.get("error"));assertTrue(view.texts.get("error").toString().contains("write rejected"));
        f.failSave=false;view.activate("save");assertEquals(1,f.saves);assertEquals(1,closed.get());
    }
    @Test public void draggingKeepsWholeViewportMathAndReleaseCapture() throws Exception {
        var f=new UiNotificationPagesTest.Fixture();var session=(NotificationUiSession)f.session("PositionPort");
        var view=new NativeUiView(new Object()){
            @Override public ScreenCoordinate bounds(String id){return id.equals("viewport")?new ScreenCoordinate(0,0,800,500):new ScreenCoordinate(75,120,32,22);}
        };session.bind(view);
        view.pointer("preview",new UiPointer(UiPointer.Kind.DOWN,80,125,0));
        view.pointer("preview",new UiPointer(UiPointer.Kind.MOVE,389,244,0));
        assertEquals(.5,session.x(),0);assertEquals(.5,session.y(),0);assertEquals(0,f.saves);
        view.pointer("preview",new UiPointer(UiPointer.Kind.UP,389,244,0));
        view.pointer("preview",new UiPointer(UiPointer.Kind.MOVE,5,5,0));assertEquals(.5,session.x(),0);
        session.detach(UiDetachReason.SWITCH);view.pointer("preview",new UiPointer(UiPointer.Kind.MOVE,0,0,0));
        assertEquals(.5,session.x(),0);assertEquals(0,view.subscriptionCount());
    }
}
