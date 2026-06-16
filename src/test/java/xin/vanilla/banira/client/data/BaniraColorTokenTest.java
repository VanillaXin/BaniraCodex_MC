package xin.vanilla.banira.client.data;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumSeason;

import static org.junit.Assert.assertNotEquals;

public class BaniraColorTokenTest {

    @Test
    public void everyTokenResolvesForAllBuiltinThemes() {
        for (EnumSeason season : concreteSeasons()) {
            assertAllTokensResolve(season.name() + "/day", BaniraColorConfig.builtinForConcreteSeason(season));
            assertAllTokensResolve(season.name() + "/night", BaniraColorConfig.builtinNightForConcreteSeason(season));
        }
    }

    private static EnumSeason[] concreteSeasons() {
        return new EnumSeason[]{EnumSeason.SPRING, EnumSeason.SUMMER, EnumSeason.AUTUMN, EnumSeason.WINTER};
    }

    private static void assertAllTokensResolve(String themeName, BaniraColorConfig theme) {
        for (BaniraColorToken token : BaniraColorToken.values()) {
            int color = theme.color(token);
            assertNotEquals(themeName + " token " + token + " resolved to transparent/unspecified color", 0, color);
        }
    }
}
