package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.*;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.internal.client.ui.page.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class UiCommonPageChromeTest {
    @org.junit.BeforeClass public static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    @org.junit.Before public void platform(){
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
        xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().registerIcon("banira_codex:common_page_test",
                xin.vanilla.banira.client.gui.quickaction.QuickIcon.resource(xin.vanilla.banira.client.data.Texture.of(
                        new net.minecraft.resources.ResourceLocation("banira_codex","gui/quick_icon.png"),64,64)),BaniraComponent.get().literal("Common"),null);
    }
    @org.junit.After public void release(){xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().unregister("banira_codex:common_page_test");}
    private Screen parent(){return new Screen(net.minecraft.network.chat.Component.empty()){};}
    private void header(NativeUiView view){assertNotNull("Registered module icon",view.gameElements.get("brand"));assertNotNull(view.texts.get("module_name"));}
    @Test public void allEightTemplatesUseCommonHeaderAndStyles() throws Exception {
        var parser=javax.xml.parsers.DocumentBuilderFactory.newInstance();parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        for(String name:new String[]{"navigation","player-config","confirm","text","item-select","effect-select","advancement-select","debug"}){
            try(var in=getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/ui/"+name+".html")){
                assertNotNull(name,in);var doc=parser.newDocumentBuilder().parse(in);var xpath=javax.xml.xpath.XPathFactory.newInstance().newXPath();
                for(String id:new String[]{"brand","title","module_name","close","content"})assertNotNull(name+"/"+id,xpath.evaluate("//*[@id='"+id+"']",doc,javax.xml.xpath.XPathConstants.NODE));
                assertNotNull(name,xpath.evaluate("//link[@href='config.css']",doc,javax.xml.xpath.XPathConstants.NODE));
            }
        }
    }
    @Test public void dialogHeaderCancelAndOldActionsCannotResolveTwice(){
        var yes=new AtomicInteger();var no=new AtomicInteger();var closes=new AtomicInteger();
        var s=new DialogUiSession(new ConfirmDialogScreen.Args().onConfirm(yes::incrementAndGet).onCancel(no::incrementAndGet));
        var old=new NativeUiView(new Object());s.bind(old);header(old);
        var current=new NativeUiView(new Object());current.onClose(closes::incrementAndGet);s.bind(current);
        old.activate("confirm");assertEquals(0,yes.get());current.activate("close");current.activate("confirm");s.close(UiCloseReason.CANCEL);
        assertEquals(0,yes.get());assertEquals(1,no.get());assertEquals(1,closes.get());
    }
    @Test public void dialogDisconnectRemainsSilent(){var n=new AtomicInteger();var s=new DialogUiSession(new ConfirmDialogScreen.Args().onCancel(n::incrementAndGet));s.bind(new NativeUiView(new Object()));s.close(UiCloseReason.DISCONNECT);s.cancel();assertEquals(0,n.get());}
    @Test public void textFooterRetainsCustomLabelAndLiteralRichParagraphs(){
        var original=BaniraComponent.get().literal("<tag>literal</tag>");var s=new TextUiSession(new ReadOnlyTextScreen.Args().paragraphs(List.of(original,BaniraComponent.get().literal("Second"))).closeText(BaniraComponent.get().literal("Return")));
        var view=new NativeUiView(new Object());var closes=new AtomicInteger();view.onClose(closes::incrementAndGet);s.bind(view);header(view);
        assertEquals("Return",view.texts.get("footer_close").toString());assertEquals("<tag>literal</tag>\n\nSecond",view.texts.get("text").toString());assertEquals("<tag>literal</tag>",original.toString());
        view.activate("footer_close");assertEquals(1,closes.get());s.detach(UiDetachReason.SWITCH);view.activate("close");assertEquals(1,closes.get());
    }
    @Test public void navigationUsesCompactRowsWithoutChangingStableKeys(){
        var calls=new AtomicInteger();var s=new NavigationUiSession(BaniraComponent.get().literal("Navigation"),List.of(new NavigationUiSession.Action("stable",BaniraComponent.get().literal("Open"),calls::incrementAndGet,null)));
        var v=new NativeUiView(new Object());s.bind(v);header(v);var rows=v.rowProviders.get("fields");assertTrue(rows instanceof UiRowPresentation);assertEquals(24,((UiRowPresentation)rows).rowHeight());assertEquals("stable",rows.key(0));
        var row=new NativeUiView(new Object());rows.bind(row,0);row.activate("open");assertEquals(1,calls.get());s.detach(UiDetachReason.SWITCH);row.activate("open");assertEquals(1,calls.get());
    }
    @Test public void defaultPlayerConfigKeepsRawInvalidDraftAndClearSearch(){
        var s=new PlayerConfigUiSession(new InputFormScreen.Args().setParentScreen(parent()).setCallback(r->fail("Invalid form submitted"))
                .addWidget(new InputFormScreen.Widget().name("amount").title(Text.literal("Amount")).type(InputFormScreen.WidgetType.NUMERIC).defaultValue("2")));
        var v=new NativeUiView(new Object());s.bind(v);header(v);assertTrue(v.rowProviders.get("fields") instanceof UiRowPresentation);assertEquals(28,((UiRowPresentation)v.rowProviders.get("fields")).rowHeight());
        s.fields().draft(0,"-");s.query("missing");assertFalse(s.fields().canSubmit());v.activate("clear_search");assertEquals("",s.query());assertEquals("-",s.fields().draft(0));assertEquals(1,v.rowProviders.get("fields").size());
    }
    @Test public void allSelectorKindsExposeCommonHeaderAndRetireClose(){
        for(var s:List.of(new SelectorUiSession(new ItemSelectScreen.Args().parentScreen(parent()).onDataReceived1(v->{})),new SelectorUiSession(new EffectSelectScreen.Args().parentScreen(parent()).onDataReceived1(v->{})),new SelectorUiSession(new AdvancementSelectScreen.Args().parentScreen(parent()).onDataReceived1(v->{})))){
            var v=new NativeUiView(new Object());var n=new AtomicInteger();v.onClose(n::incrementAndGet);s.bind(v);header(v);v.activate("close");assertEquals(1,n.get());s.detach(UiDetachReason.SWITCH);v.activate("close");assertEquals(1,n.get());
        }
    }
    @Test public void debugCommonHeaderDoesNotChangeItsActionRouting(){var n=new AtomicInteger();var s=new DebugUiSession(a->{if(a.equals("input"))n.incrementAndGet();});var v=new NativeUiView(new Object());s.bind(v);header(v);v.activate("input");assertEquals(1,n.get());s.detach(UiDetachReason.SWITCH);v.activate("input");assertEquals(1,n.get());}
}
