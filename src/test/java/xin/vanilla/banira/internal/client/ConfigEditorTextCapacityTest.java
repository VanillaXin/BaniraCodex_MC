package xin.vanilla.banira.internal.client;

import org.junit.Test;
import xin.vanilla.banira.client.gui.widget.InputWidget;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ConfigEditorTextCapacityTest {
    private InputWidget input(String initial) throws Exception {
        Class<?> type = Class.forName("sun.misc.Unsafe");
        Field instance = type.getDeclaredField("theUnsafe");
        instance.setAccessible(true);
        InputWidget input = (InputWidget) type.getMethod("allocateInstance", Class.class).invoke(instance.get(null), InputWidget.class);
        for (String name : new String[]{"undoHistory", "redoHistory"}) {
            Field field = InputWidget.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(input, new ArrayDeque<String>());
        }
        input.editable(true);
        input.maxLength(ConfigEditorInputLimits.STRING_VALUE);
        input.value(initial);
        input.setCursorPosition(initial.length());
        Field highlight = InputWidget.class.getDeclaredField("highlightPos");
        highlight.setAccessible(true);
        highlight.setInt(input, initial.length());
        return input;
    }
    private static String text(int size) { char[] chars = new char[size]; Arrays.fill(chars, 'x'); return new String(chars); }

    @Test public void editingLongConfigurationDoesNotTruncateExistingContent() throws Exception {
        String original = text(6000);
        InputWidget input = input(original);
        input.insertText("!");
        assertEquals(original + "!", input.value());
    }

    @Test public void configurationTextRemainsBoundedAt8192Characters() throws Exception {
        InputWidget input = input(text(8191));
        input.insertText("ab");
        assertEquals(text(8191) + "a", input.value());
    }
}
