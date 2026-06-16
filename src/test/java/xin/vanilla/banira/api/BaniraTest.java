package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.BaniraCodex;

import static org.junit.Assert.assertEquals;

public class BaniraTest {
    @Test
    public void exposesStableModId() {
        assertEquals("banira_codex", Banira.MOD_ID);
        assertEquals(Banira.MOD_ID, BaniraCodex.MODID);
    }
}
