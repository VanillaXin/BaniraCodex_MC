package xin.vanilla.banira.internal.config;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigViewBaselineTest {
    @Test
    public void emptyAndNullStringsKeepTheirPerFieldMeaning() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        GeneratedConfigSchemaTest.bind(CommonConfig.class, fixture.holder);
        CommonConfigView view = CommonConfigView.get();
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
        GeneratedConfigSchemaTest.bind(ClientConfig.class, fixture.holder);
        ClientConfigView view = ClientConfigView.get();
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
        GeneratedConfigSchemaTest.bind(CommonConfig.class, null);
        baseline.put("unbound", GeneratedConfigSchemaTest.read(CommonConfigView.get()));
        GeneratedConfigSchemaTest.bind(CommonConfig.class, fixture.holder);
        baseline.put("defaults", GeneratedConfigSchemaTest.read(CommonConfigView.get()));
        fixture.nonDefaultValues();
        baseline.put("changed", GeneratedConfigSchemaTest.read(CommonConfigView.get()));
        ConfigBaselineFixture.assertSnapshot("common", baseline);
    }

    @Test
    public void clientPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("schema", fixture.schema());
        GeneratedConfigSchemaTest.bind(ClientConfig.class, null);
        baseline.put("unbound", GeneratedConfigSchemaTest.read(ClientConfigView.get()));
        GeneratedConfigSchemaTest.bind(ClientConfig.class, fixture.holder);
        baseline.put("defaults", GeneratedConfigSchemaTest.read(ClientConfigView.get()));
        fixture.nonDefaultValues();
        baseline.put("changed", GeneratedConfigSchemaTest.read(ClientConfigView.get()));
        ConfigBaselineFixture.assertSnapshot("client", baseline);
    }
}
