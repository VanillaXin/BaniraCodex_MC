package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiPageSource;
import xin.vanilla.banira.client.gui.AdvancementSelectScreen;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.EffectSelectScreen;
import xin.vanilla.banira.client.gui.ItemSelectScreen;

import static org.junit.Assert.assertTrue;

public class Forge19SelectorRoutingTest {
    @Test
    public void itemSelectorExportsSharedPage() {
        assertTrue(UiPageSource.class.isAssignableFrom(ItemSelectScreen.class));
    }

    @Test
    public void effectSelectorExportsSharedPage() {
        assertTrue(UiPageSource.class.isAssignableFrom(EffectSelectScreen.class));
    }

    @Test
    public void advancementSelectorExportsSharedPage() {
        assertTrue(UiPageSource.class.isAssignableFrom(AdvancementSelectScreen.class));
    }

    @Test
    public void existingConfigEditorKeepsSharedPage() {
        assertTrue(UiPageSource.class.isAssignableFrom(ConfigEditorScreen.class));
    }
}
