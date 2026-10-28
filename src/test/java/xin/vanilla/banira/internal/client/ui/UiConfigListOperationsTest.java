package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiConfigListOperationsTest {
    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private static NativeUiView row(NativeUiView root, int index) {
        NativeUiView row = new NativeUiView(new Object());
        root.rowProviders.get("fields").bind(row, index);
        return row;
    }

    private static NativeUiView bind(UiConfigSessionTest.Fixture f) throws Exception {
        NativeUiView root = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        return root;
    }

    @Test
    public void clearUndoAndSavePreserveEveryListTypeAndDuplicateOrder() throws Exception {
        Object[][] cases = {
                {ConfigValueType.STRING_LIST, List.of("a,b", "a,b", "quote \"")},
                {ConfigValueType.INTEGER_LIST, List.of(2, 1, 2)},
                {ConfigValueType.LONG_LIST, List.of(9007199254740993L, 1L, 9007199254740993L)},
                {ConfigValueType.DOUBLE_LIST, List.of(2.25, 1.0, 2.25)},
                {ConfigValueType.BOOLEAN_LIST, List.of(true, false, true)},
                {ConfigValueType.ENUM_LIST, List.of(UiConfigSessionTest.Mode.SECOND, UiConfigSessionTest.Mode.FIRST)}
        };
        for (Object[] test : cases) {
            var f = new UiConfigSessionTest.Fixture();
            List<?> original = (List<?>) test[1];
            f.field("values", (ConfigValueType) test[0], original);
            f.open();
            NativeUiView first = bind(f);
            row(first, 0).activate("delete");
            assertEquals(List.of(), f.value("values"));
            assertEquals(original, f.holder.get("values"));
            NativeUiView restored = bind(f);
            row(restored, 0).activate("delete");
            assertEquals(String.valueOf(test[0]), original, f.value("values"));
            assertEquals(true, call(f.session, "save"));
            assertEquals(0, f.saves.get());
            row(restored, 0).activate("delete");
            assertEquals(true, call(f.session, "save"));
            assertEquals(List.of(), f.holder.get("values"));
            assertEquals(1, f.saves.get());
            call(f.session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        }
    }

    @Test
    public void staleRowsCannotApplyOrDeleteAnotherRebuiltItem() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("names", ConfigValueType.STRING_LIST, List.of("first", "second"));
        f.open();
        NativeUiView root = bind(f);
        row(root, 0).activate("open");
        NativeUiView stale = row(root, 1);
        stale.inputDraft("input", "changed");
        stale.activate("apply");
        stale.activate("delete");
        stale.activate("apply");
        assertEquals(List.of("changed", "second"), f.value("names"));
        row(root, 2).activate("delete");
        assertEquals(List.of("changed"), f.value("names"));
        row(root, 2).inputDraft("input", "new,entry");
        assertEquals(false, call(f.session, "save"));
        NativeUiView next = bind(f);
        NativeUiView pending = row(next, 2);
        assertEquals("new,entry", pending.drafts.get("input"));
        pending.activate("delete");
        assertEquals(true, call(f.session, "save"));
        assertEquals(List.of("changed"), f.holder.get("names"));
    }

    @Test
    public void keyListClearUndoResetAndSaveKeepOriginalHolderUntilCommit() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("keys", ConfigValueType.STRING_LIST, List.of("Tab", "LeftControl+K", "Tab"));
        f.descriptors.set(0, xin.vanilla.banira.common.config.ConfigEntryDescriptor.builder()
                .path("keys").displayName("keys").valueType(ConfigValueType.STRING_LIST)
                .defaultValue(List.of("Tab")).keyChords(true).build());
        f.open();
        NativeUiView root = bind(f);
        row(root, 0).activate("delete");
        row(root, 0).activate("delete");
        assertEquals(List.of("Tab", "LeftControl+K", "Tab"), f.value("keys"));
        row(root, 0).activate("reset");
        assertEquals(List.of("Tab"), f.value("keys"));
        assertEquals(List.of("Tab", "LeftControl+K", "Tab"), f.holder.get("keys"));
        assertEquals(true, call(f.session, "save"));
        assertEquals(List.of("Tab"), f.holder.get("keys"));
        assertEquals(1, f.saves.get());
    }

    @Test
    public void emptyCapturedChordRetainsAnUnboundEntryAndRoundTripsThroughClearUndo() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("keys", ConfigValueType.STRING_LIST, List.of("Tab"));
        f.descriptors.set(0, xin.vanilla.banira.common.config.ConfigEntryDescriptor.builder()
                .path("keys").displayName("keys").valueType(ConfigValueType.STRING_LIST)
                .defaultValue(List.of("Tab")).keyChords(true).build());
        f.open();
        NativeUiView root = bind(f);
        row(root, 0).activate("open");
        NativeUiView edit = row(root, 1);
        edit.input("input", UiValue.keyChords(List.of()));
        edit.activate("apply");
        assertEquals(List.of(""), f.value("keys"));
        assertEquals(true, call(f.session, "save"));
        NativeUiView add = row(root, 2);
        add.input("input", UiValue.keyChords(List.of()));
        add.activate("apply");
        assertEquals(List.of("", ""), f.value("keys"));
        assertEquals(true, call(f.session, "save"));
        assertEquals(2, f.saves.get());
        row(root, 0).activate("delete");
        row(root, 0).activate("delete");
        assertEquals(List.of("", ""), f.value("keys"));
    }
}
