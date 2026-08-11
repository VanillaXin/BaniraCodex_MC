package xin.vanilla.banira.client.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumSeason;

import static org.junit.Assert.*;

public class BaniraColorThemeLoaderTest {

    @Test
    public void dayNightFormatReadsIndependentLayers() {
        JsonObject root = jsonObject("{"
                + "\"accent\":\"FF000001\","
                + "\"day\":{\"accent\":\"FF112233\",\"tooltipUseTexture\":\"false\"},"
                + "\"night\":{\"accent\":\"FF445566\",\"tooltipUseTexture\":true}"
                + "}");

        BaniraColorThemeLoader.SeasonThemePair pair = BaniraColorThemeLoader.parseThemeRoot(EnumSeason.SPRING, root);

        assertEquals(0xFF112233, pair.day.accent());
        assertFalse(pair.day.tooltipUseTexture());
        assertEquals(0xFF445566, pair.night.accent());
        assertTrue(pair.night.tooltipUseTexture());
    }

    @Test
    public void legacyRootObjectStillAppliesToDayOnly() {
        JsonObject root = jsonObject("{\"accent\":\"FF778899\"}");

        BaniraColorThemeLoader.SeasonThemePair pair = BaniraColorThemeLoader.parseThemeRoot(EnumSeason.SUMMER, root);

        assertEquals(0xFF778899, pair.day.accent());
        assertEquals(BaniraColorConfig.builtinNightForConcreteSeason(EnumSeason.SUMMER).accent(), pair.night.accent());
    }

    @Test
    public void invalidAndZeroColorsDoNotOverrideExistingValues() {
        BaniraColorConfig theme = BaniraColorConfig.builtinForConcreteSeason(EnumSeason.AUTUMN);
        int originalAccent = theme.accent();
        int originalText = theme.textPrimary();
        JsonObject overlay = jsonObject("{\"accent\":\"bad-color\",\"textPrimary\":\"0\"}");

        BaniraColorThemeLoader.applyThemeJsonOverlay(theme, overlay);

        assertEquals(originalAccent, theme.accent());
        assertEquals(originalText, theme.textPrimary());
    }

    @Test
    public void parsesSupportedColorForms() {
        assertEquals(0xFFAABBCC, BaniraColorThemeLoader.parseColorElement(new JsonPrimitive("#FFAABBCC")));
        assertEquals(0x11223344, BaniraColorThemeLoader.parseColorElement(new JsonPrimitive("0x11223344")));
        assertEquals(0x55667788, BaniraColorThemeLoader.parseColorElement(new JsonPrimitive(0x55667788)));
    }

    @Test
    public void tooltipUseTextureSupportsBooleanAndStringValues() {
        BaniraColorConfig theme = BaniraColorConfig.builtinForConcreteSeason(EnumSeason.WINTER);

        BaniraColorThemeLoader.applyThemeJsonOverlay(theme, jsonObject("{\"tooltipUseTexture\":\"false\"}"));
        assertFalse(theme.tooltipUseTexture());

        BaniraColorThemeLoader.applyThemeJsonOverlay(theme, jsonObject("{\"tooltipUseTexture\":true}"));
        assertTrue(theme.tooltipUseTexture());
    }

    private static JsonObject jsonObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
