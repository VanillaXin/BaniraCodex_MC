package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.screen.Screen;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.ConfirmDialogScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.ReadOnlyTextScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.*;

public class UiCommonPagesTest {

    @Test
    public void nativeConfigurationOwnershipDoesNotDependOnAuiBeingInstalled() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/ui/CommonUiPages.class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            boolean nativeConfig = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods)
                if (method.name.equals("tick")) {
                    for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions)
                        if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode && ((org.objectweb.asm.tree.TypeInsnNode) instruction).getOpcode() == org.objectweb.asm.Opcodes.INSTANCEOF && ((org.objectweb.asm.tree.TypeInsnNode) instruction).desc.equals("xin/vanilla/banira/client/gui/ConfigEditorScreen"))
                            nativeConfig = true;
                }
            assertTrue("Native config sessions require the shared disconnect owner", nativeConfig);
        }
    }

    @Test
    public void clientLogoutClosesOwnedSessionsBeforeTheNextTick() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            boolean registered = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods)
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions)
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode && ((org.objectweb.asm.tree.MethodInsnNode) instruction).owner.equals("xin/vanilla/banira/client/event/BaniraClientEventHub$Player") && ((org.objectweb.asm.tree.MethodInsnNode) instruction).name.equals("onClientLoggedOut"))
                        registered = true;
            assertTrue("Logout must not wait for an observed disconnected tick", registered);
        }
    }

    @Before
    public void platform() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    private void requireMethod(Class<?> owner, String name) {
        assertTrue("Missing shared page method " + name, Arrays.stream(owner.getMethods()).anyMatch(method -> method.getName().equals(name)));
    }

    @Test
    public void nativeFormAndAuiPageOwnExactlyTheSameSession() throws Exception {
        requireMethod(InputFormScreen.class, "uiPage");
        requireMethod(InputFormScreen.class, "uiSession");
        AtomicInteger submitted = new AtomicInteger();
        InputFormScreen screen = new InputFormScreen(new InputFormScreen.Args().setParentScreen(new Screen(new net.minecraft.util.text.StringTextComponent("")) {
        }).setCallback(result -> submitted.incrementAndGet()).addWidget(new InputFormScreen.Widget().name("value").title(Text.literal("Value")).defaultValue("initial")));
        UiPageSpec page = (UiPageSpec) call(screen, "uiPage");
        assertSame(call(screen, "uiSession"), page.session());
        assertSame(screen, page.nativeFactory().create(null));
        assertEquals(0, submitted.get());
    }

    @Test
    public void duplicateNamesStillBindAndValidateByIndex() throws Exception {
        InputFormScreen.Args args = new InputFormScreen.Args().setParentScreen(new Screen(new net.minecraft.util.text.StringTextComponent("")) {
        }).setCallback(result -> {
        }).addWidget(new InputFormScreen.Widget().name("same").title(Text.literal("Same")).defaultValue("first")).addWidget(new InputFormScreen.Widget().name("same").title(Text.literal("Same")).defaultValue("second"));
        Object form = type("internal.client.ui.page.FormUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args);
        NativeUiView root = new NativeUiView(new Object()), row = new NativeUiView(new Object());
        call(form, "bind", new Class<?>[]{UiView.class}, root);
        root.rowProviders.get("fields").bind(row, 1);
        row.inputDraft("input", "changed");
        InputFormScreen.Results result = (InputFormScreen.Results) call(form, "results");
        assertEquals("first", result.value(0));
        assertEquals("changed", result.value(1));
    }

    @Test
    public void keyCaptureValueIsStructuredInsteadOfText() throws Exception {
        InputFormScreen.Args args = new InputFormScreen.Args().setParentScreen(new Screen(new net.minecraft.util.text.StringTextComponent("")) {
        }).setCallback(result -> {
        }).addWidget(new InputFormScreen.Widget().name("key").title(Text.literal("Key")).type(InputFormScreen.WidgetType.KEY_CHORD).defaultValue("LeftControl+K"));
        Object form = type("internal.client.ui.page.FormUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args);
        UiValue value = (UiValue) call(form, "get", new Class<?>[]{String.class}, "key");
        assertEquals(UiValue.Kind.KEY_CHORDS, value.kind());
        assertEquals(Arrays.asList("LeftControl", "K"), value.asKeyChords());
    }

    @Test
    public void dialogDetachmentRejectsOldActionsAndKeepsCancelExactlyOnce() throws Exception {
        AtomicInteger confirm = new AtomicInteger(), cancel = new AtomicInteger();
        Object dialog = type("internal.client.ui.page.DialogUiSession").getConstructor(ConfirmDialogScreen.Args.class).newInstance(new ConfirmDialogScreen.Args().onConfirm(confirm::incrementAndGet).onCancel(cancel::incrementAndGet));
        NativeUiView old = new NativeUiView(new Object());
        call(dialog, "bind", new Class<?>[]{UiView.class}, old);
        call(dialog, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        old.activate("confirm");
        assertEquals(0, confirm.get());
        call(dialog, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.USER_CLOSE);
        call(dialog, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.USER_CLOSE);
        assertEquals(1, cancel.get());
        assertEquals(0, old.subscriptionCount());
    }

    @Test
    public void textPagePreservesAllParagraphsAndHasOneCloseAction() throws Exception {
        Class<?> session = type("internal.client.ui.page.TextUiSession");
        Object text = session.getConstructor(ReadOnlyTextScreen.Args.class).newInstance(new ReadOnlyTextScreen.Args().paragraphs(Arrays.asList(xin.vanilla.banira.BaniraComponent.get().literal("first"), xin.vanilla.banira.BaniraComponent.get().literal("<script>last</script>"))));
        NativeUiView view = new NativeUiView(new Object());
        AtomicInteger closes = new AtomicInteger();
        view.onClose(closes::incrementAndGet);
        call(text, "bind", new Class<?>[]{UiView.class}, view);
        assertTrue(view.texts.get("text").getString("en_us").contains("<script>last</script>"));
        view.activate("close");
        assertEquals(1, closes.get());
    }

    @Test
    public void clientBootstrapRoutesCommonPagesOnSafeTick() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/banira/internal/client/BaniraCodexClientBootstrap.class")) {
            org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node, 0);
            boolean routed = false;
            for (org.objectweb.asm.tree.MethodNode method : node.methods) {
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode) {
                        org.objectweb.asm.tree.MethodInsnNode call = (org.objectweb.asm.tree.MethodInsnNode) instruction;
                        if (call.owner.equals("xin/vanilla/banira/internal/client/ui/CommonUiPages") && call.name.equals("tick"))
                            routed = true;
                    }
                }
            }
            assertTrue("Direct common Screen constructors need real END-tick routing", routed);
        }
    }

    @Test
    public void formVisibilityConditionClosesBothBackends() throws Exception {
        InputFormScreen.Args args = new InputFormScreen.Args().setParentScreen(new Screen(new net.minecraft.util.text.StringTextComponent("")) {
        }).setCallback(result -> {
        }).setInvisible(() -> true).addWidget(new InputFormScreen.Widget().name("value").title(Text.literal("Value")));
        Object form = type("internal.client.ui.page.FormUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args);
        NativeUiView view = new NativeUiView(new Object());
        AtomicInteger closes = new AtomicInteger();
        view.onClose(closes::incrementAndGet);
        call(form, "bind", new Class<?>[]{UiView.class}, view);
        requireMethod(form.getClass(), "tick");
        call(form, "tick");
        assertEquals(1, closes.get());
    }

    @Test
    public void disconnectDoesNotInvokeUserCancelCallback() throws Exception {
        AtomicInteger cancels = new AtomicInteger();
        Object dialog = type("internal.client.ui.page.DialogUiSession").getConstructor(ConfirmDialogScreen.Args.class).newInstance(new ConfirmDialogScreen.Args().onCancel(cancels::incrementAndGet));
        call(dialog, "close", new Class<?>[]{UiCloseReason.class}, UiCloseReason.DISCONNECT);
        assertEquals(0, cancels.get());
    }

    @Test
    public void nativeOnlyFormEligibilityDoesNotScanEveryTick() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        InputFormScreen.Widget widget = new InputFormScreen.Widget() {

            @Override
            public java.util.function.Consumer<InputFormScreen.Inputs> changed() {
                reads.incrementAndGet();
                return value -> {
                };
            }
        };
        widget.name("value").title(Text.literal("Value"));
        InputFormScreen screen = new InputFormScreen(new InputFormScreen.Args().setParentScreen(new Screen(new net.minecraft.util.text.StringTextComponent("")) {
        }).setCallback(result -> {
        }).addWidget(widget));
        for (int i = 0; i < 100; i++) assertNull(call(screen, "uiPage"));
        assertEquals(1, reads.get());
    }

    @Test
    public void nullTextParagraphMatchesNativeSkippingBehavior() throws Exception {
        Object session = type("internal.client.ui.page.TextUiSession").getConstructor(ReadOnlyTextScreen.Args.class).newInstance(new ReadOnlyTextScreen.Args().paragraphs(Arrays.asList(null, Text.literal("last").toComponent())));
        NativeUiView view = new NativeUiView(new Object());
        try {
            call(session, "bind", new Class<?>[]{UiView.class}, view);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            fail("Native-compatible paragraph failed: " + failure.getCause());
        }
        assertTrue(view.texts.get("text").getString("en_us").contains("last"));
    }

    @Test
    public void initialNativeStateReachesFirstAuiView() throws Exception {
        Fixture fixture = new Fixture();
        assertTrue("Missing initial-state handoff", Arrays.stream(fixture.host.getClass().getMethods()).anyMatch(method -> method.getName().equals("open") && method.getParameterCount() == 3));
        UiViewState state = new UiViewState();
        state.rowPosition(40);
        state.selection("field:40/input", 6, 2);
        call(fixture.host, "open", new Class<?>[]{UiPageSpec.class, Object.class, UiViewState.class}, fixture.page, null, state);
        assertSame(state, call(call(fixture.host, "view"), "state"));
        assertEquals(40d, ((UiViewState) call(call(fixture.host, "view"), "state")).rowPosition(), .001);
    }
}
