package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiValue;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiSelectKeyboardTest {

    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        new UiDomControlsTest().platform();
    }

    private static final class Fixture {

        final UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();

        final NativeUiView view = dom.view();

        final List<UiValue> values = new ArrayList<>();

        Fixture(boolean multiple, DropdownInputMode mode, int count) throws Exception {
            List<DropdownOption> options = new ArrayList<>();
            for (int index = 0; index < count; index++) options.add(new DropdownOption("value:" + index));
            view.control("text", new UiControlSpec(UiControlSpec.Kind.SELECT, 256, "", multiple, options, mode));
            view.value("text", multiple ? UiValue.strings(com.google.common.collect.ImmutableList.of()) : UiValue.text("value:0"));
            view.onValue("text", values::add);
            dom.ids.get("text").emit("click");
        }

        UiDomControlsTest.Dom.Node node(String css) {
            return dom.created.stream().filter(value -> css.equals(value.attributes.get("class")) && !value.removed).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        }

        void key(int code) {
            node("popup-search").key("keydown", code);
        }
    }

    @Test
    public void downAndEnterConfirmExactlyOneSelection() throws Exception {
        Fixture f = new Fixture(false, DropdownInputMode.SELECTION_ONLY, 4);
        lombok.var search = f.node("popup-search");
        f.key(264);
        f.key(257);
        assertEquals(1, f.values.size());
        assertEquals("value:1", f.values.get(0).asText());
        assertTrue(f.dom.ids.get("text").focused);
        search.key("keydown", 257);
        assertEquals(1, f.values.size());
    }

    @Test
    public void endAndHomeNavigateTheVirtualWindowWithoutAllocatingAllOptions() throws Exception {
        Fixture f = new Fixture(false, DropdownInputMode.SELECTION_ONLY, 5000);
        f.key(269);
        assertTrue("End must reveal the last option", f.node("popup-options").scroll >= 4999 * 22 - 100);
        assertTrue(f.dom.created.size() < 200);
        f.key(268);
        f.key(265);
        f.key(257);
        assertEquals("value:0", f.values.get(0).asText());
    }

    @Test
    public void filteredEnterUsesTheCurrentResultAndEmptyResultDoesNothing() throws Exception {
        Fixture f = new Fixture(false, DropdownInputMode.SELECTION_ONLY, 1200);
        lombok.var search = f.node("popup-search");
        search.value = "absent";
        search.emit("input");
        f.key(257);
        assertTrue(f.values.isEmpty());
        search.value = "value:1199";
        search.emit("input");
        f.key(257);
        assertEquals(1, f.values.size());
        assertEquals("value:1199", f.values.get(0).asText());
    }

    @Test
    public void escapeRestoresAnchorFocusAndDropsLateEvents() throws Exception {
        Fixture f = new Fixture(false, DropdownInputMode.SELECTION_ONLY, 4);
        lombok.var search = f.node("popup-search");
        f.key(264);
        f.key(256);
        assertTrue("Escape must restore the originating control", f.dom.ids.get("text").focused);
        search.key("keydown", 257);
        assertTrue(f.values.isEmpty());
    }

    @Test
    public void editableEnterKeepsCustomText() throws Exception {
        Fixture f = new Fixture(false, DropdownInputMode.EDITABLE, 3);
        lombok.var search = f.node("popup-search");
        search.value = "custom:value";
        search.emit("input");
        f.key(257);
        assertEquals("custom:value", f.values.get(0).asText());
    }

    @Test
    public void multiSelectEnterTogglesCurrentOptionAndEscapeClosesWithoutAnotherChange() throws Exception {
        Fixture f = new Fixture(true, DropdownInputMode.SELECTION_ONLY, 3);
        f.key(257);
        assertEquals(1, f.values.size());
        assertEquals(com.google.common.collect.ImmutableList.of("value:0"), f.values.get(0).asStrings());
        f.key(264);
        f.key(257);
        assertEquals(2, f.values.size());
        assertEquals(com.google.common.collect.ImmutableList.of("value:0", "value:1"), f.values.get(1).asStrings());
        f.key(256);
        assertEquals(2, f.values.size());
    }

    @Test
    public void focusedOptionCanAdvanceRepeatedlyWithoutResettingToItsOriginalSlot() throws Exception {
        Fixture f = new Fixture(false, DropdownInputMode.SELECTION_ONLY, 4);
        lombok.var option = f.node("popup-option");
        option.emit("focus");
        option.key("keydown", 264);
        option.key("keydown", 264);
        option.key("keydown", 257);
        assertEquals(1, f.values.size());
        assertEquals("value:2", f.values.get(0).asText());
    }

    @Test
    public void actualAdapterAllowsTheActiveOptionAttributeWithoutOpeningTheAttributeBoundary() throws Exception {
        lombok.var node = new org.objectweb.asm.tree.ClassNode();
        try (lombok.var stream = getClass().getResourceAsStream("/xin/vanilla/banira/internal/compat/aui/AuiDocumentAccess$AuiNode.class")) {
            new org.objectweb.asm.ClassReader(stream).accept(node, 0);
        }
        lombok.var method = node.methods.stream().filter(value -> value.name.equals("attribute")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        Set<String> literals = new HashSet<>();
        for (lombok.var instruction : method.instructions) if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode && ((org.objectweb.asm.tree.LdcInsnNode) instruction).cst instanceof String)
            literals.add((String) ((org.objectweb.asm.tree.LdcInsnNode) instruction).cst);
        assertTrue("The actual adapter must accept the active option attribute", literals.contains("aria-current"));
        assertTrue("Unknown attributes must still be rejected", literals.contains("Unsupported UI attribute"));
    }
}
