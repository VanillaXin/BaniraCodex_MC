package xin.vanilla.banira.internal.config;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

import static org.junit.Assert.assertEquals;

public class ClientConfigViewTest {
    @Test
    public void externalInventoryButtonHostDefaultsToBanira() {
        GeneratedConfigSchemaTest.bind(ClientConfig.class, null);
        assertEquals(EnumExternalInventoryButtonHost.BANIRA,
                ClientConfigView.get().externalInventoryButtonHost());
    }

    @Test
    public void externalInventoryButtonHostUsesTheConfigHolder() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        GeneratedConfigSchemaTest.bind(ClientConfig.class, fixture.holder);
        ClientConfigView view = ClientConfigView.get();
        assertEquals(EnumExternalInventoryButtonHost.BANIRA, view.externalInventoryButtonHost());
        view.externalInventoryButtonHost(EnumExternalInventoryButtonHost.FTB_LIBRARY);
        assertEquals(EnumExternalInventoryButtonHost.FTB_LIBRARY,
                fixture.values.get("externalInventoryButtonHost"));
    }
}
