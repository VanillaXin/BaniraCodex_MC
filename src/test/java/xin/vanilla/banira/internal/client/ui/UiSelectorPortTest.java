package xin.vanilla.banira.internal.client.ui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.EffectSelectScreen;
import xin.vanilla.banira.client.gui.ItemSelectScreen;
import xin.vanilla.banira.internal.client.ui.page.SelectorUiSession;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraRegistryService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.*;

public class UiSelectorPortTest {
    @org.junit.Rule
    public final xin.vanilla.banira.test.NativeClientRule nativeClient = new xin.vanilla.banira.test.NativeClientRule();

    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(FabricBaniraRegistryService.INSTANCE));
        xin.vanilla.banira.common.util.ItemUtils.clearCache();
        xin.vanilla.banira.common.util.EffectUtils.clearCache();
    }

    @After
    public void cleanup() {
        xin.vanilla.banira.common.util.ItemUtils.clearCache();
        xin.vanilla.banira.common.util.EffectUtils.clearCache();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void itemEditorKeepsCustomDataAndQuantityWithMinecraft21Components() {
        var session = new SelectorUiSession(new ItemSelectScreen.Args().parentScreen(UiSelectorParityTest.parent())
                .defaultItem(new ItemStack(Items.STONE, 11)).onDataReceived1(item -> fail("Editing must not submit the selector")));
        var editor = session.editor("nbt");
        editor.draft(0, "{broken");
        assertFalse(editor.submit());
        editor.draft(0, "{}");
        assertFalse(editor.submit());
        editor.draft(0, "{marker:42}");
        assertTrue(editor.submit());
        assertEquals(42, session.item().get(DataComponents.CUSTOM_DATA).copyTag().getInt("marker").orElseThrow().intValue());
        var itemEditor = session.editor("item");
        itemEditor.draft(0, "minecraft:diamond");
        assertTrue(itemEditor.submit());
        assertEquals(Items.DIAMOND, session.item().getItem());
        assertEquals(11, session.item().getCount());
        assertEquals(42, session.item().get(DataComponents.CUSTOM_DATA).copyTag().getInt("marker").orElseThrow().intValue());
    }

    @Test
    public void effectSelectionAndFormKeepRegistryHoldersAndClockPrecision() {
        var session = new SelectorUiSession(new EffectSelectScreen.Args().parentScreen(UiSelectorParityTest.parent())
                .defaultEffect(new MobEffectInstance(MobEffects.LUCK, 601, 3)).onDataReceived1(effect -> fail("Editing must not submit the selector")));
        session.select("minecraft:strength");
        assertEquals(MobEffects.STRENGTH, session.effect().getEffect());
        var editor = session.editor("effect");
        editor.draft(0, "minecraft:speed");
        assertTrue(editor.submit());
        assertEquals(MobEffects.SPEED, session.effect().getEffect());
        assertEquals(601, session.effect().getDuration());
        assertEquals(3, session.effect().getAmplifier());
        assertEquals("0:00:30.05", session.editor("duration").draft(0));
    }

    private int calls(String name, String owner, String method) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        int count = 0;
        for (var member : node.methods)
            for (var instruction : member.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.endsWith(owner) && call.name.equals(method))
                    count++;
        return count;
    }

    @Test
    public void advancementIconsKeepTheExistingListRenderingGuardInBothBackends() throws Exception {
        assertEquals(3, calls("client/gui/AdvancementSelectScreen", "/AdvancementData", "iconStackForListRendering"));
        assertEquals(2, calls("internal/client/ui/page/SelectorUiSession", "/AdvancementData", "iconStackForListRendering"));
    }

    @Test
    public void durationLabelsUseClockUnitsInBothLanguages() {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            String label = BaniraComponent.get().transClientAuto("enter_effect_duration").languageCode(language).getString(language);
            String error = BaniraComponent.get().transClientAuto("enter_effect_duration_error", "bad").languageCode(language).getString(language);
            assertFalse(label.contains("banira_codex."));
            assertFalse(error.contains("banira_codex."));
            assertTrue(language + ": " + label, language.equals("en_us") ? label.contains("h:mm:ss") : label.contains("时:分:秒"));
            assertTrue(error.contains("720:00:00"));
            assertFalse(label.contains("ticks") || label.contains("刻"));
        }
    }

    @Test
    public void selectorsReuseApprovedLightweightLocalResources() throws Exception {
        var resources = UiBoundaryChecks.ownResources("banira_codex");
        assertTrue(UiBoundaryChecks.resources(resources, true).isEmpty());
        for (String page : new String[]{"item", "effect", "advancement"}) {
            String html = new String(resources.get("ui/" + page + "-select.html"), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(html.contains("selector-tools"));
            assertTrue(html.contains("selected_icon"));
            assertFalse(html.contains("<script"));
        }
    }
}
