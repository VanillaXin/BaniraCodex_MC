package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.common.config.ConfigCategoryTitleSpec;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiConfigBindingTest {
    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void realRowsEditResetAndKeepFullDescriptionWithoutTouchingHolder() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        f.field("name", ConfigEntryDescriptor.ConfigValueType.STRING, "initial");
        f.open();
        NativeUiView root = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        UiRowProvider rows = root.rowProviders.get("fields");
        assertNotNull("Configuration must bind actual editable rows", rows);
        assertEquals(1, rows.size());
        NativeUiView row = new NativeUiView(new Object());
        rows.bind(row, 0);
        assertTrue(row.texts.get("tooltip").getString("en_us").contains("Complete description name"));
        row.inputDraft("input", "changed");
        assertEquals("changed", f.value("name"));
        assertEquals("initial", f.holder.get("name"));
        row.activate("reset");
        assertEquals("initial", f.value("name"));
        call(f.session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        row.inputDraft("input", "stale");
        assertEquals("initial", f.value("name"));
        assertEquals(0, row.subscriptionCount());
    }

    @Test
    public void localizedGroupSearchIncludesDescendantsAndRestoresDraftOnRefilter() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        f.field("group.number", ConfigEntryDescriptor.ConfigValueType.INTEGER, 1);
        f.field("other.text", ConfigEntryDescriptor.ConfigValueType.STRING, "other");
        f.titles.put("group", ConfigCategoryTitleSpec.literal("Advanced settings"));
        f.open();
        f.draft("group.number", "-");
        assertEquals(Collections.singletonList("group.number"), call(f.session, "search", new Class<?>[]{String.class}, "Advanced"));
        call(f.session, "search", new Class<?>[]{String.class}, "other");
        assertEquals("-", call(f.session, "draft", new Class<?>[]{String.class}, "group.number"));
    }

    @Test
    public void pendingInvalidDraftIsDetectedWithoutNumericValueCallback() {
        ConfigEditorStateFixture f = new ConfigEditorStateFixture();
        f.entry.draft = "-";
        f.entry.valid = false;
        assertEquals(1, f.state.pendingChangeCount());
    }

    @Test
    public void rendererPreferenceSwitchesOnceOnlyAfterSuccessfulSave() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        f.field("useAui", ConfigEntryDescriptor.ConfigValueType.BOOLEAN, true);
        f.open();
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        f.holder.onSaved(paths -> backend.requestSwitch((Boolean) f.holder.get("useAui")));
        f.draft("useAui", "false");
        assertTrue(backend.wantsAui("banira_codex:config"));
        f.store.failSave = true;
        assertEquals(false, call(f.session, "save"));
        backend.tick();
        assertTrue(backend.wantsAui("banira_codex:config"));
        assertEquals(0L, backend.revision());
        f.store.failSave = false;
        assertEquals(true, call(f.session, "save"));
        backend.tick();
        assertFalse(backend.wantsAui("banira_codex:config"));
        assertEquals(1L, backend.revision());
        call(f.session, "save");
        backend.tick();
        assertEquals(1L, backend.revision());
    }

    @Test
    public void nativeResetStagesValueWithoutWritingHolder() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ConfigEditorRowFactory.class")) {
            lombok.var node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            for (lombok.var method : node.methods)
                for (lombok.var instruction : method.instructions) {
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                        lombok.var invoke = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                        assertFalse("Native reset must use the shared staged edit", invoke.owner.equals("xin/vanilla/banira/common/config/ConfigHolder")
                                && invoke.name.equals("set"));
                    }
                }
        }
    }

    @Test
    public void primaryConfigEntryOpensSharedSpecBeforeNativeWidgets() throws Exception {
        boolean sharedOpen = false;
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraClientScreenService.class")) {
            lombok.var node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            for (lombok.var method : node.methods)
                if (method.name.equals("openConfigEditor")) for (lombok.var instruction : method.instructions) {
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                        lombok.var invoke = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                        sharedOpen |= invoke.owner.equals("xin/vanilla/banira/internal/client/ui/UiSessionHost") && invoke.name.equals("open");
                    }
                }
        }
        assertTrue("The AUI open path must not build all native configuration widgets first", sharedOpen);
    }

    @Test
    public void configurationNameSearchIncludesAllFields() throws Exception {
        lombok.var f = new UiConfigSessionTest.Fixture();
        f.field("group.one", ConfigEntryDescriptor.ConfigValueType.STRING, "a");
        f.field("other.two", ConfigEntryDescriptor.ConfigValueType.INTEGER, 2);
        f.open();
        assertEquals(Arrays.asList("group.one", "other.two"), call(f.session, "search", new Class<?>[]{String.class}, f.holder.getConfigName()));
    }

    @Test
    public void touchedSyncCollectsRealEditsEvenWithoutNumericCallback() {
        lombok.var f = new ConfigEditorStateFixture();
        f.entry.value = 2;
        assertEquals(Collections.singletonMap("second", 2), f.state.collectTouchedPathsForSync());
    }

    private static final class ConfigEditorStateFixture {
        final xin.vanilla.banira.internal.client.ConfigEditorState state =
                new xin.vanilla.banira.internal.client.ConfigEditorState(UiConfigParityTest.holder(new UiConfigParityTest.Store()));
        final UiConfigParityTest.Entry entry = new UiConfigParityTest.Entry(1);

        ConfigEditorStateFixture() {
            state.registerEntry("second", entry);
        }
    }
}
