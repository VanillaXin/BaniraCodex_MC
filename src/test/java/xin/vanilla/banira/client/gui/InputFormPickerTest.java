package xin.vanilla.banira.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class InputFormPickerTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    @Test public void requiresPickerCallback() {
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(new Screen(new TextComponent("parent")) {})
                .setCallback(results -> {})
                .addWidget(new InputFormScreen.Widget().name("item")
                        .type(InputFormScreen.WidgetType.PICKER));
        assertThrows(IllegalArgumentException.class, args::validate);
    }

    @Test public void pickerKeepsStructuredCallbackAndParent() {
        Screen parent = new Screen(new TextComponent("parent")) {};
        AtomicReference<String> selected = new AtomicReference<>();
        InputFormScreen.Widget widget = new InputFormScreen.Widget().name("item")
                .type(InputFormScreen.WidgetType.PICKER)
                .picker((screen, callback) -> { callback.accept("selected"); return screen; });
        assertSame(parent, widget.picker().apply(parent, selected::set));
        assertEquals("selected", selected.get());
    }
}
