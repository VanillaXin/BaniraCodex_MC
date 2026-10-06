package xin.vanilla.banira.client.gui;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class InputFormScreenStructuredValuesTest {

    @Test
    public void resultsKeepMultiSelectValuesStructured() {
        InputFormScreen.Results results = new InputFormScreen.Results()
                .values("choices", 1, List.of("a,b", " c ", ""));

        assertEquals(List.of("a,b", " c ", ""), results.values("choices"));
        assertEquals(List.of("a,b", " c ", ""), results.values(1));
        assertThrows(IllegalStateException.class, () -> results.value("choices"));
        assertThrows(IllegalStateException.class, () -> results.value(1));
        assertFalse(results.isEmpty());
    }

    @Test
    public void widgetKeepsDefaultMultiSelectValuesStructured() {
        InputFormScreen.Widget widget = new InputFormScreen.Widget()
                .type(InputFormScreen.WidgetType.DROPDOWN)
                .dropdownMultiSelect(true)
                .defaultValues(List.of("a,b", " c ", ""));

        assertEquals(List.of("a,b", " c ", ""), widget.dropdownDefaultValues());
    }

    @Test
    public void legacyVarargsDefaultValueKeepsElementBoundaries() {
        InputFormScreen.Widget widget = new InputFormScreen.Widget()
                .type(InputFormScreen.WidgetType.DROPDOWN)
                .dropdownMultiSelect(true)
                .defaultValue("a,b", " c ", "");

        assertEquals(List.of("a,b", " c ", ""), widget.dropdownDefaultValues());
    }

    @Test
    public void scalarDefaultsAndLastValueKeepTheirOriginalSemantics() {
        InputFormScreen.Widget textWidget = new InputFormScreen.Widget()
                .defaultValue("a", "b");
        InputFormScreen.Results results = new InputFormScreen.Results()
                .value("first", 0, "first")
                .values("middle", 1, List.of("a,b"))
                .value("last", 2, "last");

        assertEquals("a, b", textWidget.defaultValue());
        assertEquals("last", results.lastValue());
    }
}
