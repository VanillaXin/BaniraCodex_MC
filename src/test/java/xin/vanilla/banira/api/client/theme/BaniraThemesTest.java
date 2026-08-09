package xin.vanilla.banira.api.client.theme;

import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumSeason;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaniraThemesTest {
    private static final String MOD_ID = "theme_test";

    @After
    public void cleanup() {
        BaniraThemes.unregister(MOD_ID);
    }

    @Test
    public void defaultsToFollowingBaniraAndMapsFixedSeasons() {
        assertEquals(BaniraThemeMode.FOLLOW_BANIRA, BaniraThemes.modeFor(MOD_ID));
        assertEquals(EnumSeason.AUTO, BaniraThemes.seasonFor(MOD_ID));
        assertEquals(EnumSeason.SPRING, BaniraThemes.seasonFor(BaniraThemeMode.SPRING));
        assertEquals(EnumSeason.SUMMER, BaniraThemes.seasonFor(BaniraThemeMode.SUMMER));
        assertEquals(EnumSeason.AUTUMN, BaniraThemes.seasonFor(BaniraThemeMode.AUTUMN));
        assertEquals(EnumSeason.WINTER, BaniraThemes.seasonFor(BaniraThemeMode.WINTER));
    }

    @Test
    public void readsChildModPreferenceDynamically() {
        AtomicReference<BaniraThemeMode> mode =
                new AtomicReference<>(BaniraThemeMode.SPRING);
        BaniraThemes.register(" Theme_Test ", mode::get);

        assertEquals(EnumSeason.SPRING, BaniraThemes.seasonFor(MOD_ID));
        mode.set(BaniraThemeMode.WINTER);
        assertEquals(EnumSeason.WINTER, BaniraThemes.seasonFor(MOD_ID));
        assertTrue(BaniraThemes.unregister(MOD_ID));
        assertFalse(BaniraThemes.unregister(MOD_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankModIds() {
        BaniraThemes.modeFor("  ");
    }
}
