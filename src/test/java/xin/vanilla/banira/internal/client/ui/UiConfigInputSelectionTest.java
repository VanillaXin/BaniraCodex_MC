package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor.ConfigValueType;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;
import java.util.List;
import static org.junit.Assert.*;
import static xin.vanilla.banira.internal.client.ui.UiSessionHandoffTest.call;

public class UiConfigInputSelectionTest {
    @BeforeClass
    public static void bootstrap() { UiDomControlsTest.bootstrap(); }

    @Before
    public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }

    private UiDomControlsTest.Dom list() throws Exception {
        lombok.var fixture = new UiConfigSessionTest.Fixture();
        fixture.field("names", ConfigValueType.STRING_LIST, com.google.common.collect.ImmutableList.of("first"));
        fixture.open();
        fixture.draft("names", "{\"expanded\":true}");
        lombok.var dom = new UiDomControlsTest.Dom();
        call(fixture.session, "bind", new Class<?>[]{UiView.class}, dom.view());
        return dom;
    }

    @Test
    public void listAddKeepsTheNewCaretDuringValidationRefresh() throws Exception {
        lombok.var dom = list();
        lombok.var input = dom.created.stream().filter(n -> n.tag.equals("input") && n.value.isEmpty()).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        input.focused = true;
        input.emit("focus");
        input.value = "n";
        input.start = input.end = 1;
        input.emit("input");
        assertEquals("n", input.value);
        assertEquals(1, input.start);
        assertEquals(1, input.end);
    }

    @Test
    public void listReplacementKeepsTheCollapsedSelectionAfterTyping() throws Exception {
        lombok.var dom = list();
        lombok.var input = dom.created.stream().filter(n -> n.tag.equals("input") && n.value.equals("first")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        input.focused = true;
        input.start = 1;
        input.end = 4;
        input.emit("select");
        input.value = "fXXt";
        input.start = input.end = 3;
        input.emit("input");
        assertEquals("fXXt", input.value);
        assertEquals(3, input.start);
        assertEquals(3, input.end);
    }

    @Test
    public void reentrantTextRefreshKeepsTheCurrentInputSelection() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var view = dom.view();
        view.control("text", new UiControlSpec(UiControlSpec.Kind.TEXT, 128, "", false));
        view.draft("text", "");
        view.onDraft("text", value -> view.enabled("text", true));
        lombok.var input = dom.ids.get("text");
        input.focused = true;
        input.emit("focus");
        input.value = "typed";
        input.start = input.end = 5;
        input.emit("input");
        assertEquals(5, input.start);
        assertEquals(5, input.end);
    }

    @Test
    public void restoredRowsReapplySelectionAfterLayoutWithoutFightingLaterInput() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var view = (UiDomView) dom.view();
        view.state().selection("retained/input", 3, 3);
        view.rows("fields", new UiRowProvider() {
            public int size() { return 1; }
            public String key(int index) { return "retained"; }
            public void bind(UiView row, int index) {
                row.control("input", new UiControlSpec(UiControlSpec.Kind.TEXT, 128, "", false));
                row.draft("input", "draft");
            }
        }, 120);
        view.afterLayout();
        lombok.var input = dom.created.stream().filter(n -> n.tag.equals("input")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        input.start = input.end = 0;
        input.rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(10, 40, 180, 20);
        view.afterLayout();
        assertEquals(3, input.start);
        assertEquals(3, input.end);
        input.start = input.end = 1;
        input.emit("select");
        view.afterLayout();
        assertEquals(1, input.start);
        assertEquals(1, input.end);
    }

    @Test
    public void unfocusedInputDoesNotStealAnotherControlsSelection() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var view = dom.view();
        view.control("text", new UiControlSpec(UiControlSpec.Kind.TEXT, 128, "", false));
        view.state().selection("other", 2, 4);
        lombok.var input = dom.ids.get("text");
        input.value = "typed";
        input.start = input.end = 5;
        input.emit("input");
        assertEquals("other", view.state().focusedField());
        assertEquals(2, view.state().selectionStart());
        assertEquals(4, view.state().selectionEnd());
    }
}
