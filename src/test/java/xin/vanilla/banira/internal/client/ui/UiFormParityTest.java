package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.screens.Screen;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.gui.ConfirmDialogScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.type;

public class UiFormParityTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @org.junit.Before
    public void installPlatform() {
        xin.vanilla.banira.platform.BaniraPlatforms.install(new xin.vanilla.banira.platform.TestBaniraPlatform());
    }

    private InputFormScreen.Args args(java.util.function.Consumer<InputFormScreen.Results> callback) {
        return new InputFormScreen.Args().setParentScreen(new Screen(net.minecraft.network.chat.Component.empty()) {
                })
                .setCallback(callback)
                .addWidget(new InputFormScreen.Widget().name("text").title(Text.literal("Text"))
                        .defaultValue("<script>payload()</script>"))
                .addWidget(new InputFormScreen.Widget().name("amount").title(Text.literal("Amount"))
                        .type(InputFormScreen.WidgetType.NUMERIC).defaultValue("2.50"))
                .addWidget(new InputFormScreen.Widget().name("tags").title(Text.literal("Tags"))
                        .type(InputFormScreen.WidgetType.DROPDOWN).dropdownOptions("a,b", "z")
                        .dropdownMultiSelect(true).defaultValues("a,b", "z"));
    }

    private Object form(InputFormScreen.Args args) throws Exception {
        return type("internal.client.ui.page.FormUiSession").getConstructor(InputFormScreen.Args.class).newInstance(args);
    }

    @Test
    public void typedValuesKeepStructuredSelectionsAndLiteralText() throws Exception {
        Object form = form(args(result -> {
        }));
        assertEquals("<script>payload()</script>", ((UiValue) call(form, "get", new Class<?>[]{String.class}, "text")).asText());
        assertEquals(new BigDecimal("2.50"), ((UiValue) call(form, "get", new Class<?>[]{String.class}, "amount")).asDecimal());
        InputFormScreen.Results result = (InputFormScreen.Results) call(form, "results");
        assertEquals(Arrays.asList("a,b", "z"), result.values("tags"));
        try {
            result.value("tags");
            fail("Structured selections were flattened");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void submitUsesExistingValidatorAndCallbackFailureContract() throws Exception {
        AtomicInteger submissions = new AtomicInteger();
        InputFormScreen.Args args = args(result -> {
            submissions.incrementAndGet();
            result.runningResult("rejected");
        });
        args.getWidgets().get(0).validator(result -> result.value("text").equals("bad") ? "invalid" : "");
        Object form = form(args);
        call(form, "set", new Class<?>[]{String.class, UiValue.class}, "text", UiValue.text("bad"));
        assertEquals(false, call(form, "submit"));
        assertEquals(0, submissions.get());
        call(form, "set", new Class<?>[]{String.class, UiValue.class}, "text", UiValue.text("good"));
        assertEquals(false, call(form, "submit"));
        assertEquals(1, submissions.get());
        assertEquals("rejected", call(form, "error"));
    }

    @Test
    public void backendDetachDoesNotSubmitOrCancel() throws Exception {
        AtomicReference<InputFormScreen.Results> result = new AtomicReference<>();
        Object form = form(args(result::set));
        call(form, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        assertNull(result.get());
        assertEquals("<script>payload()</script>", ((UiValue) call(form, "get", new Class<?>[]{String.class}, "text")).asText());
    }

    @Test
    public void confirmationCallbacksAreExactlyOnceAcrossDetachAndClose() throws Exception {
        AtomicInteger confirmed = new AtomicInteger(), cancelled = new AtomicInteger();
        Object dialog = type("internal.client.ui.page.DialogUiSession")
                .getConstructor(ConfirmDialogScreen.Args.class)
                .newInstance(new ConfirmDialogScreen.Args().onConfirm(confirmed::incrementAndGet).onCancel(cancelled::incrementAndGet));
        call(dialog, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        assertEquals(0, cancelled.get());
        call(dialog, "confirm");
        call(dialog, "confirm");
        call(dialog, "cancel");
        assertEquals(1, confirmed.get());
        assertEquals(0, cancelled.get());
    }

    @Test
    public void controlsRetainNonValuePresentationTypes() throws Exception {
        InputFormScreen.Args args = args(result -> {
        });
        args.addWidget(new InputFormScreen.Widget().name("password").title(Text.literal("Password"))
                .type(InputFormScreen.WidgetType.PASSWORD));
        args.addWidget(new InputFormScreen.Widget().name("file").title(Text.literal("File"))
                .type(InputFormScreen.WidgetType.FILE));
        args.addWidget(new InputFormScreen.Widget().name("color").title(Text.literal("Color"))
                .type(InputFormScreen.WidgetType.COLOR).defaultValue("#80ffffff"));
        Object form = form(args);
        for (String[] expected : new String[][]{{"password", "PASSWORD"}, {"file", "FILE"}, {"color", "COLOR"}, {"tags", "SELECT"}}) {
            Object control = call(form, "control", new Class<?>[]{String.class}, expected[0]);
            assertEquals(expected[1], call(control, "kind").toString());
        }
    }

    @Test
    public void boundFormUsesSharedRowsAndSubmitAction() throws Exception {
        AtomicInteger submissions = new AtomicInteger(), closes = new AtomicInteger();
        Object form = form(args(result -> submissions.incrementAndGet()));
        NativeUiView view = new NativeUiView(new Object());
        view.onClose(closes::incrementAndGet);
        call(form, "bind", new Class<?>[]{xin.vanilla.banira.api.client.ui.UiView.class}, view);
        assertTrue("Form must bind actual rows", view.rowProviders.containsKey("fields"));
        assertEquals(3, view.rowProviders.get("fields").size());
        view.activate("submit");
        assertEquals(1, submissions.get());
        assertEquals(1, closes.get());
        view.activate("submit");
        assertEquals(1, submissions.get());
        assertEquals(1, closes.get());
    }

    @Test
    public void partialNumericDraftSurvivesDetachWithoutBecomingTextValue() throws Exception {
        Object form = form(args(result -> {
        }));
        call(form, "draft", new Class<?>[]{String.class, String.class}, "amount", "-");
        call(form, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        assertEquals("-", call(form, "draft", new Class<?>[]{String.class}, "amount"));
        assertEquals(false, call(form, "submit"));
        try {
            call(form, "get", new Class<?>[]{String.class}, "amount");
            fail("Invalid decimal was converted to a text value");
        } catch (java.lang.reflect.InvocationTargetException invalid) {
            assertTrue(invalid.getCause() instanceof NumberFormatException);
        }
    }

    @Test
    public void detachedFormEventsCannotSubmitRetainedBusinessState() throws Exception {
        AtomicInteger submissions = new AtomicInteger();
        Object form = form(args(result -> submissions.incrementAndGet()));
        NativeUiView old = new NativeUiView(new Object());
        call(form, "bind", new Class<?>[]{xin.vanilla.banira.api.client.ui.UiView.class}, old);
        call(form, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        old.activate("submit");
        assertEquals(0, submissions.get());
        assertEquals(0, old.subscriptionCount());
    }

    @Test
    public void recursiveSubmitCannotDuplicateCallback() throws Exception {
        AtomicInteger submissions = new AtomicInteger();
        AtomicReference<Object> form = new AtomicReference<>();
        AtomicReference<Boolean> nested = new AtomicReference<>();
        form.set(form(args(result -> {
            if (submissions.incrementAndGet() == 1) {
                try {
                    nested.set((Boolean) call(form.get(), "submit"));
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            }
        })));
        assertEquals(true, call(form.get(), "submit"));
        assertEquals(false, nested.get());
        assertEquals(1, submissions.get());
    }

    @Test
    public void numericRowBindingPreservesPartialDraftAndDropsOldEvents() throws Exception {
        Object form = form(args(result -> {
        }));
        NativeUiView view = new NativeUiView(new Object());
        call(form, "bind", new Class<?>[]{xin.vanilla.banira.api.client.ui.UiView.class}, view);
        assertTrue("Missing form row bindings", view.rowProviders.containsKey("fields"));
        NativeUiView row = new NativeUiView(new Object());
        view.rowProviders.get("fields").bind(row, 1);
        assertTrue("Partial drafts require their own input channel",
                Arrays.stream(row.getClass().getMethods()).anyMatch(method -> method.getName().equals("inputDraft")));
        call(row, "inputDraft", new Class<?>[]{String.class, String.class}, "input", "-");
        assertEquals("-", call(form, "draft", new Class<?>[]{String.class}, "amount"));
        call(form, "detach", new Class<?>[]{UiDetachReason.class}, UiDetachReason.SWITCH);
        call(row, "inputDraft", new Class<?>[]{String.class, String.class}, "input", "125");
        assertEquals("-", call(form, "draft", new Class<?>[]{String.class}, "amount"));
        assertEquals(0, row.subscriptionCount());
    }

    @Test
    public void incompleteNumberNeverReachesDomainValidator() throws Exception {
        AtomicInteger validations = new AtomicInteger();
        InputFormScreen.Args args = args(result -> {
        });
        args.getWidgets().get(1).validator(result -> {
            validations.incrementAndGet();
            new BigDecimal(result.value("amount"));
            return "";
        });
        Object form = form(args);
        call(form, "draft", new Class<?>[]{String.class, String.class}, "amount", "-");
        try {
            assertEquals(false, call(form, "canSubmit"));
        } catch (java.lang.reflect.InvocationTargetException failure) {
            fail("Incomplete input reached domain validator: " + failure.getCause());
        }
        assertEquals(0, validations.get());
    }
}
