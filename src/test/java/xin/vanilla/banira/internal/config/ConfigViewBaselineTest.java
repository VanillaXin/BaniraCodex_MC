package xin.vanilla.banira.internal.config;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import xin.vanilla.banira.common.enums.EnumSeason;
import static org.junit.Assert.*;

public class ConfigViewBaselineTest {
    @Test
    public void emptyAndNullStringsKeepTheirPerFieldMeaning() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        CommonConfig.RootView view = CommonConfigAccess.root(fixture.holder);
        fixture.values.put("help.helpHeader", "");
        fixture.values.put("command.commandPrefix", "");
        assertEquals("", view.help().helpHeader());
        assertEquals("banira", view.command().commandPrefix());
        fixture.values.put("help.helpHeader", null);
        fixture.values.put("command.commandLanguage", null);
        fixture.values.put("language.defaultLanguage", null);
        assertNull(view.help().helpHeader());
        assertNull(view.command().commandLanguage());
        assertEquals("en_us", view.language().defaultLanguage());
        assertEquals(0, fixture.saves);
    }

    @Test
    public void clientConversionStillAcceptsLegacyNumbersAndUnknownEnums() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        ClientConfig.RootView view = ClientConfigAccess.root(fixture.holder);
        fixture.values.put("guiThemeStyle", "not-a-season");
        fixture.values.put("notificationLogMaxEntries", 17L);
        assertEquals(EnumSeason.AUTO, view.guiThemeStyle());
        assertEquals(17, view.notificationLogMaxEntries());
        fixture.values.put("notificationLogMaxEntries", null);
        assertEquals(500, view.notificationLogMaxEntries());
        view.notificationLogMaxEntries(42);
        assertEquals(42, fixture.values.get("notificationLogMaxEntries"));
        assertEquals(0, fixture.saves);
        fixture.holder.save();
        assertEquals(1, fixture.saves);
    }

    @Test
    public void commonPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("schema", fixture.schema());
        baseline.put("unbound", ConfigBaselineFixture.readView(CommonConfigAccess.root(null), CommonConfig.RootView.class));
        baseline.put("defaults", ConfigBaselineFixture.readView(CommonConfigAccess.root(fixture.holder), CommonConfig.RootView.class));
        fixture.nonDefaultValues();
        baseline.put("changed", ConfigBaselineFixture.readView(CommonConfigAccess.root(fixture.holder), CommonConfig.RootView.class));
        ConfigBaselineFixture.assertSnapshot("common", baseline);
    }

    @Test
    public void clientPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("schema", fixture.schema());
        baseline.put("unbound", ConfigBaselineFixture.readView(ClientConfigAccess.root(null), ClientConfig.RootView.class));
        baseline.put("defaults", ConfigBaselineFixture.readView(ClientConfigAccess.root(fixture.holder), ClientConfig.RootView.class));
        fixture.nonDefaultValues();
        baseline.put("changed", ConfigBaselineFixture.readView(ClientConfigAccess.root(fixture.holder), ClientConfig.RootView.class));
        ConfigBaselineFixture.assertSnapshot("client", baseline);
    }
}
