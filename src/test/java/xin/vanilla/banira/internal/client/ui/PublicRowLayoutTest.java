package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import static org.junit.Assert.*;

public class PublicRowLayoutTest {
    @Test public void downstreamCanSelectRowGeometryWithoutInternalTypes() throws Exception {
        Class<?> api;
        try { api = Class.forName("xin.vanilla.banira.api.client.ui.UiRowLayout"); }
        catch (ClassNotFoundException absent) { fail("Missing public row layout contract"); return; }
        assertTrue(UiRowProvider.class.isAssignableFrom(api));
        assertEquals(int.class, api.getMethod("rowHeight").getReturnType());
        assertEquals(String.class, api.getMethod("scrollerId").getReturnType());
        assertTrue(api.isAssignableFrom(UiRowPresentation.class));
    }
    @Test public void ordinaryRowsKeepTheExistingPublicProviderContract() throws Exception {
        assertNotNull(UiRowProvider.class.getMethod("size"));
        assertNotNull(UiRowProvider.class.getMethod("key", int.class));
    }

    @Test public void publicLayoutControlsTheActualVirtualRowHeightAndStyle() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
        var dom = new UiDomControlsTest.Dom(); var view = dom.view();
        view.rows("fields", new xin.vanilla.banira.api.client.ui.UiRowLayout() {
            public int rowHeight() { return 24; }
            public String rowClass(int index) { return "public-compact"; }
            public int size() { return 20; }
            public String key(int index) { return "slot:" + index; }
            public void bind(xin.vanilla.banira.api.client.ui.UiView row, int index) { row.text("label", xin.vanilla.banira.BaniraComponent.get().literal("Slot " + index)); }
        }, 8);
        assertTrue(dom.created.stream().anyMatch(n -> n.attributes.getOrDefault("class", "").contains("public-compact")));
        assertTrue(dom.created.stream().anyMatch(n -> n.attributes.getOrDefault("style", "").contains("24px")));
        view.close();
    }
}
