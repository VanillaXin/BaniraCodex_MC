package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.config.*;
import xin.vanilla.banira.internal.client.ConfigEditorState;
import xin.vanilla.banira.platform.*;
import java.lang.reflect.Method;
import java.util.*;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiConfigListDraftTest {
    private java.lang.reflect.Field instance;
    private Object previousMinecraft;
    private xin.vanilla.banira.client.gui.BaniraScreen screen;
    @BeforeClass public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    @Before public void platform() throws Exception {
        BaniraPlatforms.install(new TestBaniraPlatform());
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        var field = unsafeType.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Object minecraft = unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), net.minecraft.client.Minecraft.class);
        for (var candidate : net.minecraft.client.Minecraft.class.getDeclaredFields()) {
            if (candidate.getType() == net.minecraft.client.Minecraft.class && java.lang.reflect.Modifier.isStatic(candidate.getModifiers())) {
                instance = candidate; instance.setAccessible(true); previousMinecraft = instance.get(null); instance.set(null, minecraft); break;
            }
        }
        assertNotNull(instance);
        screen = new xin.vanilla.banira.client.gui.BaniraScreen(net.minecraft.network.chat.Component.literal("list")) {
            @Override protected void onRender(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partialTicks) {}
        };
    }
    @After public void restoreClient() throws Exception { if (instance != null) instance.set(null, previousMinecraft); }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method;
        try { method = target.getClass().getMethod(name, types); }
        catch (NoSuchMethodException absent) { throw new AssertionError("Missing retained list draft boundary: " + name, absent); }
        return method.invoke(target, args);
    }

    @Test public void nativePendingAddAndInlineEditSurviveRecreation() throws Exception {
        TagListEditorWidget first = new TagListEditorWidget(screen);
        first.items(new ArrayList<>(Arrays.asList("a,b", "a,b")));
        invoke(first, "restoreDraft", new Class<?>[]{String.class},
                "{\"expanded\":true,\"adding\":true,\"addText\":\"unfinished,\",\"editingIndex\":1,\"editText\":\"edited\",\"scroll\":20}");
        String snapshot = (String) invoke(first, "captureDraft", new Class<?>[0]);
        TagListEditorWidget next = new TagListEditorWidget(screen);
        next.items(new ArrayList<>(first.items()));
        invoke(next, "restoreDraft", new Class<?>[]{String.class}, snapshot);
        assertEquals(snapshot, invoke(next, "captureDraft", new Class<?>[0]));
        assertEquals(Arrays.asList("a,b", "a,b"), next.items());
        assertEquals(false, invoke(next, "draftCommitted", new Class<?>[0]));
    }

    @Test public void listRowsUseTypedControlsAndPreservePendingDraftAcrossNativeHandoff() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("numbers", ConfigEntryDescriptor.ConfigValueType.LONG_LIST, Arrays.asList(1L, 1L)); f.open();
        NativeUiView root = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        NativeUiView header = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(header, 0);
        assertNotNull("Lists must expose an expandable editor, not raw JSON", header.texts.get("open"));
        header.activate("open");
        NativeUiView row = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(row, 1);
        assertEquals(UiControlSpec.Kind.DECIMAL, row.controls.get("input").kind());
        row.inputDraft("input", "-");
        assertEquals(false, call(f.session, "save"));
        ConfigEditorState state = (ConfigEditorState) call(f.session, "state");
        String draft = state.entryWidgets().get("numbers").getDraft();
        assertTrue(draft.contains("-"));
        TagListEditorWidget nativeList = new TagListEditorWidget(screen).itemType(TagListEditorWidget.ItemType.NUMBER);
        nativeList.items(new ArrayList<>((List<?>) f.value("numbers")));
        invoke(nativeList, "restoreDraft", new Class<?>[]{String.class}, draft);
        String returned = (String) invoke(nativeList, "captureDraft", new Class<?>[0]);
        f.draft("numbers", returned);
        f.holder.acceptExternalReload();
        assertEquals(draft, call(f.session, "draft", new Class<?>[]{String.class}, "numbers"));
        assertEquals(1, state.pendingChangeCount());
        row.inputDraft("input", "9007199254740993"); row.activate("apply");
        assertEquals(Arrays.asList(9007199254740993L, 1L), f.value("numbers"));
        assertEquals(true, call(f.session, "save"));
    }

    @Test public void keyListUsesCaptureAndKeepsDuplicateChords() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("keys", ConfigEntryDescriptor.ConfigValueType.STRING_LIST, Arrays.asList("TAB", "TAB"));
        f.descriptors.set(0, ConfigEntryDescriptor.builder().path("keys").displayName("keys")
                .valueType(ConfigEntryDescriptor.ConfigValueType.STRING_LIST)
                .defaultValue(Arrays.asList("TAB", "TAB")).keyChords(true).build());
        f.open(); NativeUiView root = new NativeUiView(new Object());
        call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        NativeUiView header = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(header, 0);
        assertNotNull("Key lists must open a real capture editor", header.texts.get("open")); header.activate("open");
        NativeUiView row = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(row, 1);
        assertEquals(UiControlSpec.Kind.KEY_CHORD, row.controls.get("input").kind());
        row.input("input", UiValue.keyChords(Arrays.asList("LEFT_CONTROL", "K"))); row.activate("apply");
        assertEquals(Arrays.asList("LEFT_CONTROL+K", "TAB"), f.value("keys"));
        assertEquals(Arrays.asList("TAB", "TAB"), f.holder.get("keys"));
    }

    @Test public void pendingAddMustBeConfirmedBeforeSaveOrSync() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("names", ConfigEntryDescriptor.ConfigValueType.STRING_LIST, Arrays.asList("a,b", "a,b")); f.open();
        NativeUiView root = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        NativeUiView header = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(header, 0);
        assertNotNull(header.texts.get("open")); header.activate("open");
        NativeUiView add = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(add, 3);
        add.inputDraft("input", "new,entry");
        assertEquals(false, call(f.session, "sync", new Class<?>[]{boolean.class}, true));
        assertTrue(f.sent.isEmpty());
        call(f.session, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        NativeUiView next = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, next);
        NativeUiView restored = new NativeUiView(new Object()); next.rowProviders.get("fields").bind(restored, 3);
        assertEquals("new,entry", restored.drafts.get("input"));
        restored.activate("apply");
        assertEquals(Arrays.asList("a,b", "a,b", "new,entry"), f.value("names"));
        assertEquals(true, call(f.session, "save"));
        add.activate("apply");
        assertEquals(3, ((List<?>) f.value("names")).size());
    }

    @Test public void invalidRawListSurvivesNativeFallbackWithoutBecomingClean() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("names", ConfigEntryDescriptor.ConfigValueType.STRING_LIST, Collections.singletonList("original")); f.open();
        f.draft("names", "[broken");
        String draft = (String) call(f.session, "draft", new Class<?>[]{String.class}, "names");
        TagListEditorWidget nativeList = new TagListEditorWidget(screen); nativeList.items(new ArrayList<>(Collections.singletonList("original")));
        invoke(nativeList, "restoreDraft", new Class<?>[]{String.class}, draft);
        f.draft("names", (String) invoke(nativeList, "captureDraft", new Class<?>[0]));
        assertEquals(false, call(f.session, "save"));
        assertEquals(Collections.singletonList("original"), f.value("names"));
        assertTrue(((ConfigEditorState) call(f.session, "state")).pendingChangeCount() > 0);
    }

    @Test public void nativeLongLabelsDoNotRoundThroughDouble() throws Exception {
        TagListEditorWidget widget = new TagListEditorWidget(screen);
        Method format = TagListEditorWidget.class.getDeclaredMethod("formatItemLabel", Object.class); format.setAccessible(true);
        assertEquals("9007199254740993", format.invoke(widget, 9007199254740993L));
    }

    @Test public void nativeKeyPreviewCannotLeakIntoRetainedListDraft() throws Exception {
        TagListEditorWidget tags = new TagListEditorWidget(screen).itemType(TagListEditorWidget.ItemType.KEY_CHORD);
        tags.items(new ArrayList<>(List.of("Tab")));
        InputWidget input = tags.focusInput(0); assertTrue(input instanceof KeyCaptureInputWidget);
        screen.requestFocus(input);
        var press = KeyCaptureInputWidget.class.getDeclaredMethod("onKeyPress", int.class, int.class, int.class);
        press.setAccessible(true); press.invoke(input, 341, 0, 2);
        var draft = xin.vanilla.banira.internal.client.ConfigListDraft.decode(tags.captureDraft());
        assertEquals("Tab", draft.editText);
        press.invoke(input, 256, 0, 0);
        assertEquals("Tab", input.value());
        assertEquals(List.of("Tab"), tags.items());
    }

    @Test public void booleanAddSelectionSurvivesBothBackends() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("flags", ConfigEntryDescriptor.ConfigValueType.BOOLEAN_LIST, Collections.singletonList(false)); f.open();
        NativeUiView root = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        NativeUiView header = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(header, 0); header.activate("open");
        NativeUiView add = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(add, 2);
        add.input("input", add.controls.get("input").multiple() ? UiValue.strings(Collections.singletonList("true")) : UiValue.text("true"));
        TagListEditorWidget nativeList = new TagListEditorWidget(screen).itemType(TagListEditorWidget.ItemType.BOOLEAN);
        nativeList.items(new ArrayList<>(Collections.singletonList(false)));
        nativeList.restoreDraft((String) call(f.session, "draft", new Class<?>[]{String.class}, "flags"));
        assertEquals(Collections.singletonList("true"), nativeList.findChildByType(DropdownSelectWidget.class).getSelectedValues());
        f.draft("flags", nativeList.captureDraft());
        NativeUiView next = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, next);
        NativeUiView restored = new NativeUiView(new Object()); next.rowProviders.get("fields").bind(restored, 2);
        restored.activate("apply");
        assertEquals(Arrays.asList(false, true), f.value("flags"));
    }

    @Test public void nativeMultiSelectionRetainsAllPendingValuesInOrder() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("modes", ConfigEntryDescriptor.ConfigValueType.ENUM_LIST, Collections.emptyList()); f.open();
        f.draft("modes", "{\"expanded\":true,\"adding\":true,\"selected\":[\"SECOND\",\"FIRST\"]}");
        NativeUiView root = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        NativeUiView add = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(add, 1);
        assertTrue("Native multiple selections must not collapse to one value", add.controls.get("input").multiple());
        assertEquals(Arrays.asList("SECOND", "FIRST"), add.values.get("input").asStrings());
        add.activate("apply");
        assertEquals(Arrays.asList(UiConfigSessionTest.Mode.SECOND, UiConfigSessionTest.Mode.FIRST), f.value("modes"));
    }

    @Test public void invalidNativeListRemainsWholeAndInvalidAcrossFallback() throws Exception {
        var f = new UiConfigSessionTest.Fixture();
        f.field("numbers", ConfigEntryDescriptor.ConfigValueType.INTEGER_LIST, Collections.singletonList(1)); f.open();
        var state = (ConfigEditorState) call(f.session, "state");
        var factory = new xin.vanilla.banira.internal.client.ConfigEditorRowFactory(screen, f.holder, state, () -> {});
        var adapter = factory.createEntryRow(f.descriptors.get(0), 400, 20);
        state.clearEntries(); state.registerEntry("numbers", adapter);
        var tags = adapter.getWidget().findChildByType(TagListEditorWidget.class);
        tags.items(new ArrayList<>(Arrays.asList(1L, 2147483648L)));
        assertFalse(adapter.isValid());
        assertEquals("Invalid elements must be retained, not dropped or converted to null", Arrays.asList(1L, 2147483648L), adapter.getValue());
        call(f.session, "bind", new Class<?>[]{UiView.class}, new NativeUiView(new Object()));
        assertEquals(false, call(f.session, "save"));
        assertEquals(Collections.singletonList(1), f.holder.get("numbers"));
    }

    @Test public void nativeListFocusRestoresTheSameItemAndSelection() throws Exception {
        TagListEditorWidget tags = new TagListEditorWidget(screen);
        tags.items(new ArrayList<>(Arrays.asList("first", "second")));
        Object input = invoke(tags, "focusInput", new Class<?>[]{int.class}, 1);
        assertTrue(input instanceof InputWidget);
        ((InputWidget) input).setSelectionRange(1, 4);
        screen.requestFocus((InputWidget) input);
        assertEquals(1, invoke(tags, "focusedInputIndex", new Class<?>[0]));
        assertEquals("second", ((InputWidget) input).value());
        assertEquals(1, ((InputWidget) input).cursorPosition());
        assertEquals(4, ((InputWidget) input).highlightPos());
    }

    @Test public void groupExpansionUsesTheSameRetainedMapForBothBackends() throws Exception {
        var f = new UiConfigSessionTest.Fixture(); f.field("group.name", ConfigEntryDescriptor.ConfigValueType.STRING, "value"); f.open();
        NativeUiView root = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, root);
        NativeUiView group = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(group, 0); group.activate("open");
        assertEquals(Boolean.TRUE, ((Map<?, ?>) invoke(f.session, "categoryExpansion", new Class<?>[0])).get("group"));
        invoke(f.session, "categoryExpansion", new Class<?>[]{String.class, boolean.class}, "group", false);
        NativeUiView next = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, next);
        assertEquals(1, next.rowProviders.get("fields").size());
    }

    @Test public void everyListDescriptorEditsThroughControlsAndNativeAdapters() throws Exception {
        Object[][] cases = {
            {ConfigEntryDescriptor.ConfigValueType.STRING_LIST, "a,b", "c,d", "c,d"},
            {ConfigEntryDescriptor.ConfigValueType.INTEGER_LIST, 1, "2", 2},
            {ConfigEntryDescriptor.ConfigValueType.LONG_LIST, 1L, "9007199254740993", 9007199254740993L},
            {ConfigEntryDescriptor.ConfigValueType.DOUBLE_LIST, 1.0, "2.25", 2.25},
            {ConfigEntryDescriptor.ConfigValueType.BOOLEAN_LIST, true, "false", false},
            {ConfigEntryDescriptor.ConfigValueType.ENUM_LIST, UiConfigSessionTest.Mode.FIRST, "SECOND", UiConfigSessionTest.Mode.SECOND}
        };
        for (Object[] test : cases) {
            var f = new UiConfigSessionTest.Fixture(); f.field("value", (ConfigEntryDescriptor.ConfigValueType) test[0], Collections.singletonList(test[1])); f.open();
            NativeUiView root = new NativeUiView(new Object()); call(f.session, "bind", new Class<?>[]{UiView.class}, root);
            NativeUiView header = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(header, 0); header.activate("open");
            NativeUiView row = new NativeUiView(new Object()); root.rowProviders.get("fields").bind(row, 1);
            if (row.controls.get("input").kind() == UiControlSpec.Kind.SELECT) row.input("input", UiValue.text((String) test[2]));
            else row.inputDraft("input", (String) test[2]);
            row.activate("apply"); assertEquals(Collections.singletonList(test[3]), f.value("value"));
            var state = (ConfigEditorState) call(f.session, "state");
            var adapter = new xin.vanilla.banira.internal.client.ConfigEditorRowFactory(screen, f.holder, state, () -> {}).createEntryRow(f.descriptors.get(0), 400, 20);
            state.clearEntries(); state.registerEntry("value", adapter);
            assertTrue(String.valueOf(test[0]), adapter.isValid()); assertEquals(Collections.singletonList(test[3]), adapter.getValue());
            call(f.session, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        }
    }
}
