package xin.vanilla.banira.client.gui;

import net.minecraft.client.gui.screens.Screen;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class InputFormPickerTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void requiresPickerCallback() {
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(new Screen(net.minecraft.network.chat.Component.literal("parent")) {
                })
                .setCallback(results -> {
                })
                .addWidget(new InputFormScreen.Widget().name("item")
                        .type(InputFormScreen.WidgetType.PICKER));
        assertThrows(IllegalArgumentException.class, args::validate);
    }

    @Test
    public void pickerKeepsStructuredCallbackAndParent() {
        Screen parent = new Screen(net.minecraft.network.chat.Component.literal("parent")) {
        };
        AtomicReference<String> selected = new AtomicReference<>();
        InputFormScreen.Widget widget = new InputFormScreen.Widget().name("item")
                .type(InputFormScreen.WidgetType.PICKER)
                .picker((screen, callback) -> {
                    callback.accept("selected");
                    return screen;
                });
        assertSame(parent, widget.picker().apply(parent, selected::set));
        assertEquals("selected", selected.get());
    }

    @Test
    public void nativeEditHandlerUpdatesConditionalFieldsOnTheFirstSelection() throws Exception {
        var parent = new Screen(net.minecraft.network.chat.Component.empty()) {
        };
        var args = new InputFormScreen.Args().setParentScreen(parent).setCallback(results -> {
        });
        var text = xin.vanilla.banira.client.gui.component.Text.literal("Source");
        args.addWidget(new InputFormScreen.Widget().name("source").title(text)
                .type(InputFormScreen.WidgetType.DROPDOWN).dropdownOptions("auto", "image", "item").defaultValue("auto"));
        args.addWidget(new InputFormScreen.Widget().name("image").title(text)
                .visibleWhen(values -> "image".equals(values.value("source"))));
        args.addWidget(new InputFormScreen.Widget().name("item").title(text)
                .visibleWhen(values -> "item".equals(values.value("source"))));
        var form = new InputFormScreen(args);
        var edit = InputFormScreen.class.getDeclaredMethod("validateAndUpdateError", InputFormScreen.Widget.class,
                int.class, String.class, xin.vanilla.banira.client.gui.widget.InputWidget.class);
        edit.setAccessible(true);
        var source = args.getWidgets().get(0);
        var session = form.uiSession();
        assertFalse(session.fieldVisible(1));
        assertFalse(session.fieldVisible(2));

        edit.invoke(form, source, 0, "image", null);
        assertEquals("image", session.draft("source"));
        assertTrue(session.fieldVisible(1));
        assertFalse(session.fieldVisible(2));
        assertFalse(session.canSubmit());

        edit.invoke(form, source, 0, "item", null);
        assertEquals("item", session.draft("source"));
        assertFalse(session.fieldVisible(1));
        assertTrue(session.fieldVisible(2));

        edit.invoke(form, source, 0, "auto", null);
        assertFalse(session.fieldVisible(1));
        assertFalse(session.fieldVisible(2));
        assertTrue(session.canSubmit());
    }

    @Test
    public void nativePickerReturnKeepsItsScrollAndNewDraftBeforeTheNextRender() throws Exception {
        Screen parent = new Screen(net.minecraft.network.chat.Component.literal("parent")) {
        };
        var form = new InputFormScreen(new InputFormScreen.Args().setParentScreen(parent)
                .setCallback(result -> {
                }).addWidget(new InputFormScreen.Widget().name("item").defaultValue("old")));
        var scrollbar = new xin.vanilla.banira.client.gui.widget.ScrollbarWidget(form);
        scrollbar.maxValue(250).value(150);
        var scrollbarField = InputFormScreen.class.getDeclaredField("scrollbarWidget");
        scrollbarField.setAccessible(true);
        scrollbarField.set(form, scrollbar);
        form.rememberPickerHandoff();
        var stateField = InputFormScreen.class.getDeclaredField("uiState");
        stateField.setAccessible(true);
        var state = (xin.vanilla.banira.internal.client.ui.UiViewState) stateField.get(form);
        assertTrue(state.rowPosition() > 0);
        var session = (xin.vanilla.banira.internal.client.ui.page.FormUiSession) form.pageSession();
        session.draft("item", "new");
        form.captureUiState(state);
        var restore = InputFormScreen.class.getDeclaredMethod("restoreUiDrafts");
        restore.setAccessible(true);
        restore.invoke(form);
        var values = InputFormScreen.class.getDeclaredField("inputValues");
        values.setAccessible(true);
        assertEquals(java.util.List.of("new"), values.get(form));
        assertTrue(state.rowPosition() > 0);
    }
}
