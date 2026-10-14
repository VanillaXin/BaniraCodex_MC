package xin.vanilla.banira.client.gui;

import net.minecraft.client.gui.screens.Screen;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class InputFormPickerTest {
    @org.junit.BeforeClass
    public static void bootstrap() throws Exception {
        xin.vanilla.banira.common.network.packet.NotificationToClientBudgetTest.bootstrapGame();
    }
    @Test public void requiresPickerCallback() {
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(new Screen(net.minecraft.network.chat.Component.literal("parent")) {})
                .setCallback(results -> {})
                .addWidget(new InputFormScreen.Widget().name("item")
                        .type(InputFormScreen.WidgetType.PICKER));
        assertThrows(IllegalArgumentException.class, args::validate);
    }

    @Test public void pickerKeepsStructuredCallbackAndParent() {
        Screen parent = new Screen(net.minecraft.network.chat.Component.literal("parent")) {};
        AtomicReference<String> selected = new AtomicReference<>();
        InputFormScreen.Widget widget = new InputFormScreen.Widget().name("item")
                .type(InputFormScreen.WidgetType.PICKER)
                .picker((screen, callback) -> { callback.accept("selected"); return screen; });
        assertSame(parent, widget.picker().apply(parent, selected::set));
        assertEquals("selected", selected.get());
    }
}
