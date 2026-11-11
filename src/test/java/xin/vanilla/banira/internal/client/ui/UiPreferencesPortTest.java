package xin.vanilla.banira.internal.client.ui;

import net.minecraft.world.effect.MobEffects;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.CodexNavigationScreen;
import xin.vanilla.banira.client.gui.PlayerConfigScreen;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UiPreferencesPortTest {
    @org.junit.Rule
    public final xin.vanilla.banira.test.NativeClientRule nativeClient = new xin.vanilla.banira.test.NativeClientRule();

    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private ClassNode node(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/banira/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void nativePlayerScrollerKeepsMinecraft21BothAxisContract() throws Exception {
        var scroll = node("client/gui/PlayerConfigScreen").methods.stream()
                .filter(method -> method.name.equals("mouseScrolled")).findFirst().orElseThrow();
        assertEquals("(DDDD)Z", scroll.desc);
        int delegates = 0;
        for (var instruction : scroll.instructions)
            if (instruction instanceof MethodInsnNode call && call.name.equals("mouseScrolled")) {
                assertEquals("(DDDD)Z", call.desc);
                delegates++;
            }
        assertEquals(1, delegates);
    }

    @Test
    public void realEntrancesShareSessionsWithoutForcingUnportedSubclassesToAui() throws Exception {
        for (String name : new String[]{"CustomPlayerConfigEditScreen", "CodexNavigationScreen",
                "quickaction.CustomQuickActionConfigScreen", "quickaction.CustomQuickActionStepsScreen"})
            assertTrue(name, UiNativePage.class.isAssignableFrom(Class.forName("xin.vanilla.banira.client.gui." + name)));
        var legacy = new LegacyPlayerScreen();
        assertNull(legacy.uiSession());
        assertNull(legacy.uiPage());
        var navigation = new CodexNavigationScreen(null);
        assertSame(navigation.uiSession(), navigation.uiSession());
        assertEquals(Set.of("codex_navigation_notification_log", "codex_navigation_client_config",
                        "codex_navigation_common_config", "custom_player_config_title", "custom_quick_action_title"),
                navigation.uiSession().actions().stream().map(action -> action.id()).collect(Collectors.toSet()));
        assertNotNull(navigation.uiPage());
    }

    @Test
    public void quickEffectPickerUsesRegistryHoldersAndPreservesFallback() throws Exception {
        var type = Class.forName("xin.vanilla.banira.client.gui.quickaction.CustomQuickActionStepsScreen");
        var lookup = type.getDeclaredMethod("optionalEffect", String.class);
        lookup.setAccessible(true);
        assertSame(MobEffects.SPEED.value(), lookup.invoke(null, "minecraft:speed"));
        assertSame(MobEffects.LUCK.value(), lookup.invoke(null, "invalid id"));
        assertSame(MobEffects.LUCK.value(), lookup.invoke(null, "test:missing"));
        int constructors = 0, unwraps = 0;
        for (var method : node("client/gui/quickaction/CustomQuickActionStepsScreen").methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.equals("net/minecraft/world/effect/MobEffectInstance") && call.name.equals("<init>")) {
                        assertEquals("(Lnet/minecraft/core/Holder;II)V", call.desc);
                        constructors++;
                    }
                    if (call.owner.equals("net/minecraft/core/Holder") && call.name.equals("value")) unwraps++;
                }
        assertEquals(1, constructors);
        assertTrue(unwraps > 0);
    }

    @Test
    public void preferenceSaveKeepsExistingTwoFieldWireAndSenderOwnedSnapshot() throws Exception {
        var packet = node("common/network/packet/CustomPlayerConfigSyncToServer");
        int reads = 0, writes = 0, senderIds = 0, snapshots = 0;
        for (var method : packet.methods)
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    if (call.owner.endsWith("/BaniraPacketBuffer") && call.name.equals("readUtf")) reads++;
                    if (call.owner.endsWith("/BaniraPacketBuffer") && call.name.equals("writeUtf")) writes++;
                    if (call.owner.endsWith("/ServerSenderAccess") && call.name.equals("uuidString")) senderIds++;
                    if (call.owner.endsWith("/ConfigSnapshotToClient") && call.name.equals("<init>")) snapshots++;
                }
        assertEquals(2, reads);
        assertEquals(2, writes);
        assertEquals(1, senderIds);
        assertEquals(1, snapshots);
        var lifecycle = node("internal/common/BaniraCodexRuntime").methods.stream()
                .filter(method -> method.name.equals("registerPlayerLifecycle")).findFirst().orElseThrow();
        for (var instruction : lifecycle.instructions)
            if (instruction instanceof MethodInsnNode call) assertNotEquals("onLoggedIn", call.name);
    }

    @Test
    public void sharedPlayerAndQuickLabelsResolveInBothLanguages() {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            for (String key : new String[]{"player_config_personal", "player_config_seconds", "custom_quick_action_search_hint"}) {
                String text = BaniraComponent.get().transClientAuto(key).languageCode(language).getString(language);
                assertFalse(text.isBlank());
                assertFalse(text, text.contains("banira_codex.") || text.equals(key));
            }
            String range = BaniraComponent.get().transClientAuto("player_config_seconds_range", 1, 60)
                    .languageCode(language).getString(language);
            assertTrue(range, range.contains("1") && range.contains("60"));
            assertFalse(range, range.contains("banira_codex.") || range.contains("%s"));
        }
    }

    private static final class LegacyPlayerScreen extends PlayerConfigScreen {
        LegacyPlayerScreen() {
            super("legacy", BaniraComponent.get().literal("Legacy"), null, null, null);
        }

        @Override
        protected void buildPlayerConfig(CollapsiblePanelWidget root) {
        }

        @Override
        protected void savePlayerConfig() {
        }
    }
}
