package xin.vanilla.banira.internal.client.ui;

import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.client.gui.EffectDurationModel;
import xin.vanilla.banira.internal.DebugScreen;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class UiFabric16SelectorPortTest {
    @BeforeClass
    public static void bootstrap() {

        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform().registryService(
                xin.vanilla.banira.internal.fabric.platform.FabricBaniraRegistryService.INSTANCE));
    }

    @After
    public void cleanup() {
        xin.vanilla.banira.common.util.ItemUtils.clearCache();
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private ClassNode node(String name) throws Exception {
        ClassNode node = new ClassNode();
        try (lombok.var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private boolean calls(ClassNode node, String owner, String method) {
        return node.methods.stream().flatMap(candidate -> Arrays.stream(candidate.instructions.toArray()))
                .anyMatch(instruction -> instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/" + owner) && ((MethodInsnNode) instruction).name.equals(method));
    }

    @Test
    public void allFourNativeEntrancesRetainSharedRoutingAndCleanup() throws Exception {
        for (String path : new String[]{"client/gui/ItemSelectScreen", "client/gui/EffectSelectScreen",
                "client/gui/AdvancementSelectScreen", "internal/DebugScreen"}) {
            ClassNode page = node(path);
            assertTrue(page.interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
            assertTrue(calls(page, "internal/client/ui/CommonUiPages", "close"));
            assertTrue(calls(page, "internal/client/ui/CommonUiPages", "removed"));
            assertTrue(page.methods.stream().anyMatch(method -> method.name.equals("captureUiState")));
            assertTrue(page.methods.stream().anyMatch(method -> method.name.equals("prepareUiState")));
        }
    }

    @Test
    public void nativeSelectorsSubmitAndEditThroughTheSameSession() throws Exception {
        for (String name : new String[]{"ItemSelectScreen", "EffectSelectScreen", "AdvancementSelectScreen"}) {
            ClassNode page = node("client/gui/" + name);
            assertTrue(calls(page, "internal/client/ui/page/SelectorUiSession", "spec"));
            assertTrue(calls(page, "internal/client/ui/page/SelectorUiSession", "submitNative"));
            assertTrue(calls(page, "internal/client/ui/page/SelectorUiSession", "openEditor"));
            assertFalse(page.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                    .anyMatch(instruction -> instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.startsWith("net/minecraftforge/")));
        }
    }

    @Test
    public void effectIconAndDurationEditorShareTheClockModel() throws Exception {
        assertEquals("0:00:30.05", EffectDurationModel.clockText(601));
        ClassNode icon = node("client/gui/widget/EffectIconWidget");
        for (String methodName : new String[]{"renderTooltip", "drawEffectIcon"}) {
            assertTrue(methodName, icon.methods.stream().filter(method -> method.name.equals(methodName))
                    .flatMap(method -> Arrays.stream(method.instructions.toArray()))
                    .anyMatch(instruction -> instruction instanceof MethodInsnNode
                            && ((MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/client/gui/EffectDurationModel")
                            && ((MethodInsnNode) instruction).name.equals("clockText")));
        }
        ClassNode selector = node("client/gui/EffectSelectScreen");
        assertTrue(calls(selector, "client/gui/EffectDurationModel", "clockText"));
        assertTrue(calls(node("internal/client/ui/page/SelectorUiSession"), "client/gui/EffectDurationModel", "parseTime"));
    }

    @Test
    public void effectEditorLabelsNoLongerAdvertiseTicks() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            try (lombok.var input = getClass().getResourceAsStream("/assets/banira_codex/lang/" + language + ".json")) {
                assertNotNull(language, input);
                com.google.gson.JsonObject values = new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : new String[]{"format.banira_codex.enter_effect_duration_error",
                        "format.banira_codex.set_duration", "word.banira_codex.enter_effect_duration"}) {
                    assertTrue(key, values.has(key));
                    String label = values.get(key).getAsString();
                    assertFalse(label, label.contains("tick"));
                    assertFalse(label, label.contains("刻"));
                }
            }
        }
    }

    @Test
    public void debugNativeFactoryKeepsTheExistingSessionWithoutExecutingActions() {
        DebugScreen screen = new DebugScreen();
        assertSame(screen.uiSession(), screen.uiPage().session());
        assertSame(screen, screen.uiPage().nativeFactory().create(null));
        assertEquals("banira_codex:debug", screen.uiSurfaceId());
        assertTrue(screen.uiSession().content().isEmpty());
    }
}
