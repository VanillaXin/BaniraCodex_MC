package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.screens.Screen;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiPageSource;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.internal.client.ui.page.FormUiSession;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UiSharedFormPageTest {
    @org.junit.Rule
    public final xin.vanilla.banira.test.NativeClientRule nativeClient = new xin.vanilla.banira.test.NativeClientRule();

    @org.junit.BeforeClass
    public static void bootstrap() {
        xin.vanilla.banira.common.util.RegistryKeyReuseTest.initializeStandaloneLoader();
        xin.vanilla.banira.test.NativeBootstrap.initialize();
    }

    @org.junit.Before
    public void platform() {
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
        xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().registerIcon("banira_codex:b09_header_test",
                xin.vanilla.banira.client.gui.quickaction.QuickIcon.resource(xin.vanilla.banira.client.data.Texture.of(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("banira_codex", "gui/quick_icon.png"), 64, 64)),
                xin.vanilla.banira.BaniraComponent.get().literal("Shared form"), null);
        for (String namespace : new String[]{"narcissus_farewell", "aotake_sweep", "sakura_sign_in"})
            xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().registerIcon(namespace + ":b09_header_test",
                    xin.vanilla.banira.client.gui.quickaction.QuickIcon.resource(xin.vanilla.banira.client.data.Texture.of(
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, "gui/quick_icon.png"), 64, 64)),
                    xin.vanilla.banira.BaniraComponent.get().literal(namespace), null);
    }

    @org.junit.After
    public void releaseIcon() {
        for (String namespace : new String[]{"banira_codex", "narcissus_farewell", "aotake_sweep", "sakura_sign_in"})
            xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry.get().unregister(namespace + ":b09_header_test");
    }

    private InputFormScreen.Args args(java.util.function.Consumer<InputFormScreen.Results> callback) {
        return new InputFormScreen.Args().setParentScreen(new Screen(net.minecraft.network.chat.Component.empty()) {
                })
                .setHeaderTitle(Text.literal("Shared form")).setCallback(callback)
                .addWidget(new InputFormScreen.Widget().name("literal").title(Text.literal("Text"))
                        .tooltip(Text.literal("Full description that must remain available when the label is clipped"))
                        .defaultValue("<tag>literal</tag>"))
                .addWidget(new InputFormScreen.Widget().name("amount").title(Text.literal("Amount"))
                        .type(InputFormScreen.WidgetType.NUMERIC).defaultValue("2.50")
                        .validator(r -> r.value("amount").equals("99") ? "Amount rejected" : ""));
    }


    @Test
    public void genericRowsUseCompactPresentationAndKeepValidationOnActualInput() {
        var form = new FormUiSession(args(r -> fail("Invalid submission")));
        var view = new NativeUiView(new Object());
        form.bind(view);
        assertTrue("Generic form rows must use the configuration presentation", view.rowProviders.get("fields") instanceof UiRowPresentation);
        assertEquals(28, ((UiRowPresentation) view.rowProviders.get("fields")).rowHeight());
        var row = new NativeUiView(new Object());
        view.rowProviders.get("fields").bind(row, 1);
        row.inputDraft("input", "-");
        assertEquals("-", form.draft("amount"));
        assertFalse(form.canSubmit());
        assertFalse(view.enabled.get("submit"));
        assertFalse(row.texts.get("validation").isEmpty());
        row.inputDraft("input", "99");
        assertEquals("Amount rejected", row.texts.get("validation").toString());
        row.inputDraft("input", "3.25");
        assertTrue(form.canSubmit());
        assertTrue(row.texts.get("validation").isEmpty());
    }

    @Test
    public void conditionalFieldsRebindFromTheNewValueAndKeepHiddenDrafts() {
        var args = args(r -> fail("Unexpected submission"));
        args.addWidget(new InputFormScreen.Widget().name("extra").title(Text.literal("Extra"))
                .visibleWhen(r -> "show".equals(r.value("literal"))));
        var form = new FormUiSession(args);
        var view = new NativeUiView(new Object());
        form.bind(view);
        assertEquals(2, form.visibleFieldCount());
        assertTrue(form.canSubmit());
        var row = new NativeUiView(new Object());
        view.rowProviders.get("fields").bind(row, 0);
        row.inputDraft("input", "show");
        assertEquals(3, form.visibleFieldCount());
        assertFalse(form.canSubmit());
        form.draft("extra", "retained");
        form.draft("literal", "hide");
        assertEquals(2, form.visibleFieldCount());
        form.draft("literal", "show");
        assertEquals("retained", form.draft("extra"));
        assertTrue(form.canSubmit());
    }

    @Test
    public void readOnlyPickerRejectsTextEventsButAcceptsStructuredResultsAcrossRebind() {
        var args = args(r -> fail("Unexpected submission"));
        args.addWidget(new InputFormScreen.Widget().name("summary").title(Text.literal("Summary"))
                .type(InputFormScreen.WidgetType.PICKER).pickerReadOnly(true).defaultValue("original")
                .picker((parent, callback) -> parent));
        var form = new FormUiSession(args);
        var view = new NativeUiView(new Object());
        form.bind(view);
        var row = new NativeUiView(new Object());
        view.rowProviders.get("fields").bind(row, 2);
        assertFalse(row.enabled.get("input"));
        row.inputDraft("input", "disabled edit");
        row.enabled("input", true);
        row.inputDraft("input", "stale draft");
        row.input("input", UiValue.text("stale value"));
        assertEquals("original", form.draft("summary"));
        form.draft("summary", "chosen item");
        var next = new NativeUiView(new Object());
        form.bind(next);
        var restored = new NativeUiView(new Object());
        next.rowProviders.get("fields").bind(restored, 2);
        assertEquals("chosen item", restored.drafts.get("input"));
        assertFalse(restored.enabled.get("input"));
    }

    @Test
    public void hiddenPickerCannotOpenAndDoesNotBlockSubmission() {
        AtomicInteger picks = new AtomicInteger(), submissions = new AtomicInteger();
        var args = args(r -> submissions.incrementAndGet());
        args.addWidget(new InputFormScreen.Widget().name("hidden").title(Text.literal("Hidden"))
                .type(InputFormScreen.WidgetType.PICKER).visibleWhen(r -> false)
                .validator(r -> "Hidden validation must not run")
                .picker((parent, callback) -> {
                    picks.incrementAndGet();
                    return parent;
                }));
        var form = new FormUiSession(args);
        assertFalse(form.fieldVisible(2));
        form.pick(2);
        assertEquals(0, picks.get());
        assertTrue(form.canSubmit());
        assertTrue(form.submit());
        assertEquals(1, submissions.get());
    }

    @Test
    public void wideFieldsDoNotDependOnAnOptionalCompletionProvider() throws Exception {
        var args = new InputFormScreen.Args().setTitle(Text.literal("NBT")).setParentScreen(new Screen(net.minecraft.network.chat.Component.empty()) {
                }).setCallback(result -> fail("Layout must not submit"))
                .addWidget(new InputFormScreen.Widget().title(Text.literal("NBT")).wideInput(true));
        assertNull(args.getWidgets().get(0).completion());
        var form = new FormUiSession(args);
        var view = new NativeUiView(new Object());
        form.bind(view);
        var rows = (UiRowPresentation) view.rowProviders.get("fields");
        assertEquals(48, rows.rowHeight());
        assertEquals("player-config-field wide-input-field", rows.rowClass(0));


    }

    @Test
    public void headerCloseAndRetiredActionsNeverSubmit() {
        AtomicInteger callbacks = new AtomicInteger(), closes = new AtomicInteger();
        var form = new FormUiSession(args(r -> callbacks.incrementAndGet()));
        var old = new NativeUiView(new Object());
        old.onClose(closes::incrementAndGet);
        form.bind(old);
        assertNotNull("Actual registered icon", old.gameElements.get("brand"));
        assertNotNull(old.texts.get("module_name"));
        form.draft("literal", "retained");
        var current = new NativeUiView(new Object());
        current.onClose(closes::incrementAndGet);
        form.bind(current);
        old.activate("close");
        old.activate("submit");
        assertEquals(0, callbacks.get());
        assertEquals(0, closes.get());
        current.activate("close");
        assertEquals(1, closes.get());
        assertEquals(0, callbacks.get());
        assertEquals("retained", form.draft("literal"));
    }

    @Test
    public void callbackRejectionKeepsLiteralDraftAndRetrySucceedsOnlyOnce() {
        AtomicInteger callbacks = new AtomicInteger(), closes = new AtomicInteger();
        var form = new FormUiSession(args(r -> {
            if (callbacks.incrementAndGet() == 1) r.runningResult("Rejected by callback");
        }));
        var view = new NativeUiView(new Object());
        view.onClose(closes::incrementAndGet);
        form.bind(view);
        view.activate("submit");
        assertEquals(1, callbacks.get());
        assertEquals(0, closes.get());
        assertEquals("Rejected by callback", form.error());
        assertEquals("<tag>literal</tag>", form.draft("literal"));
        view.activate("submit");
        view.activate("submit");
        assertEquals(2, callbacks.get());
        assertEquals(1, closes.get());
    }

    @Test
    public void nativeHandoffRetainsRawDraftAndDisposesOldRowEvents() {
        var form = new FormUiSession(args(r -> fail("Handoff submitted")));
        var view = new NativeUiView(new Object());
        form.bind(view);
        var row = new NativeUiView(new Object());
        view.rowProviders.get("fields").bind(row, 1);
        row.inputDraft("input", "-");
        form.detach(UiDetachReason.SWITCH);
        row.inputDraft("input", "100");
        view.activate("submit");
        assertEquals("-", form.draft("amount"));
        var current = new NativeUiView(new Object());
        form.bind(current);
        var currentRow = new NativeUiView(new Object());
        current.rowProviders.get("fields").bind(currentRow, 1);
        assertEquals("-", currentRow.drafts.get("input"));
        assertFalse(form.canSubmit());
        assertEquals(0, row.subscriptionCount());
        assertEquals(0, view.subscriptionCount());
    }

    @Test
    public void literalHeaderRetainsTheScopedWindowTitleOwner() {
        for (String namespace : new String[]{"narcissus_farewell", "aotake_sweep", "sakura_sign_in"}) {
            var form = new FormUiSession(args(r -> {
            }).setTitle(Text.transAuto(namespace, "mod_name")));
            assertEquals("Fixture must carry an actual scoped title", namespace, form.args().getTitle().toComponent().modId());
            var view = new NativeUiView(new Object());
            form.bind(view);
            assertEquals(namespace, view.gameElements.get("brand").option().texture()[0].location().getNamespace());
            form.detach(UiDetachReason.SWITCH);
        }
    }

    @Test
    public void unscopedTitlesRetainTheNativeParentSurfaceOwner() {
        var form = new FormUiSession(args(r -> {
        }).setTitle(Text.literal("Window")).setParentScreen(new OwnerScreen()));
        var view = new NativeUiView(new Object());
        form.bind(view);
        assertEquals("narcissus_farewell", view.gameElements.get("brand").option().texture()[0].location().getNamespace());
    }

    private static final class OwnerScreen extends Screen implements UiPageSource {
        private OwnerScreen() {
            super(net.minecraft.network.chat.Component.empty());
        }

        public String uiSurfaceId() {
            return "narcissus_farewell:waypoints";
        }

        public UiPageSpec uiPage() {
            throw new AssertionError("Branding must not construct the parent page");
        }
    }
}
