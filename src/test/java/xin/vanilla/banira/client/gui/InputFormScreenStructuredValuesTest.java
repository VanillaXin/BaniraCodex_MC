package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class InputFormScreenStructuredValuesTest {

    @Test
    public void resultsKeepMultiSelectValuesStructured() {
        InputFormScreen.Results results = new InputFormScreen.Results()
                .values("choices", 1, Arrays.asList("a,b", " c ", ""));

        assertEquals(Arrays.asList("a,b", " c ", ""), results.values("choices"));
        assertEquals(Arrays.asList("a,b", " c ", ""), results.values(1));
        assertFalse(results.isEmpty());
        try {
            results.value("choices");
            fail("expected structured result guard");
        } catch (IllegalStateException expected) {
            // Expected: multi-select results must use values(...).
        }
    }

    @Test
    public void defaultsKeepBothScalarAndStructuredSemantics() {
        InputFormScreen.Widget dropdown = new InputFormScreen.Widget()
                .type(InputFormScreen.WidgetType.DROPDOWN)
                .dropdownMultiSelect(true)
                .defaultValue("a,b", " c ", "");
        InputFormScreen.Widget text = new InputFormScreen.Widget().defaultValue("a", "b");

        assertEquals(Arrays.asList("a,b", " c ", ""), dropdown.dropdownDefaultValues());
        assertEquals("a, b", text.defaultValue());
    }
}
