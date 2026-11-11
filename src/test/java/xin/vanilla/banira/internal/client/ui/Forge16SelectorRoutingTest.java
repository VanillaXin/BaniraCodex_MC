package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiPageSource;
import xin.vanilla.banira.client.gui.AdvancementSelectScreen;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.EffectSelectScreen;
import xin.vanilla.banira.client.gui.ItemSelectScreen;

import static org.junit.Assert.assertTrue;

public class Forge16SelectorRoutingTest {

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

    @Test
    public void debugExportsSharedPage() {
        assertTrue(UiPageSource.class.isAssignableFrom(xin.vanilla.banira.internal.DebugScreen.class));
    }

    @Test
    public void forge16RetainsExistingEventWrappers() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            long calls = node.methods.stream().flatMap(method -> java.util.Arrays.stream(method.instructions.toArray())).filter(instruction -> instruction instanceof org.objectweb.asm.tree.MethodInsnNode && ((org.objectweb.asm.tree.MethodInsnNode) instruction).owner.startsWith("xin/vanilla/banira/client/event/BaniraClientEventHub$")).count();
            assertTrue(calls >= 5);
        }
    }
}
