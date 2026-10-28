package xin.vanilla.banira.internal.client.ui;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiControlSpec;
import xin.vanilla.banira.api.client.ui.UiValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UiDomDeletionInputTest {
    @BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @Before
    public void platform() {
        new UiDomControlsTest().platform();
    }

    @Test
    public void silentBackspaceAndDeleteSynchronizeRawDraftOnce() throws Exception {
        for (UiControlSpec.Kind kind : List.of(UiControlSpec.Kind.TEXT, UiControlSpec.Kind.PASSWORD, UiControlSpec.Kind.DECIMAL)) {
            UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
            NativeUiView view = dom.view();
            view.control("amount", new UiControlSpec(kind, 64, "", false));
            view.draft("amount", "12");
            List<String> drafts = new ArrayList<>();
            view.onDraft("amount", drafts::add);
            var node = dom.ids.get("amount");
            node.value = "1";
            node.key("keyup", 259);
            node.key("keyup", 259);
            assertEquals(List.of("1"), drafts);
            node.value = "";
            node.key("keyup", 261);
            assertEquals(List.of("1", ""), drafts);
            node.value = "-";
            node.emit("input");
            node.key("keyup", 259);
            assertEquals(List.of("1", "", "-"), drafts);
            view.enabled("amount", false);
            node.value = "blocked";
            node.key("keyup", 259);
            view.close();
            node.key("keyup", 261);
            assertEquals(List.of("1", "", "-"), drafts);
        }
    }

    @Test
    public void deletionFallbackPreservesTypedTextAndIgnoresOtherKeys() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        NativeUiView view = dom.view();
        view.value("text", UiValue.text("abc"));
        List<String> values = new ArrayList<>();
        view.onValue("text", value -> values.add(value.asText()));
        var node = dom.ids.get("text");
        node.value = "ab";
        node.key("keyup", 263);
        assertEquals(List.of(), values);
        node.key("keyup", 259);
        node.key("keyup", 259);
        assertEquals(List.of("ab"), values);
        view.close();
    }

    @Test
    public void deletingPopupSearchRestoresOptionsWithoutChoosing() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        var spec = new UiControlSpec(UiControlSpec.Kind.SELECT, 64, "", false,
                List.of(new xin.vanilla.banira.client.gui.widget.DropdownOption("alpha"),
                        new xin.vanilla.banira.client.gui.widget.DropdownOption("beta")),
                xin.vanilla.banira.client.gui.widget.DropdownInputMode.SELECTION_ONLY);
        List<UiValue> selected = new ArrayList<>();
        UiSelectPopup popup = new UiSelectPopup((UiDomAccess) dom.access,
                (UiDomAccess.Node) dom.ids.get("text").proxy, spec, UiValue.text(""), selected::add,
                () -> dom.current, () -> {
        }, failure -> {
            throw new AssertionError(failure);
        });
        var search = dom.created.stream().filter(node -> "popup-search".equals(node.attributes.get("class")))
                .findFirst().orElseThrow();
        search.value = "alpha";
        search.emit("input");
        assertEquals(1L, visibleOptions(dom));
        search.value = "";
        search.key("keyup", 259);
        assertEquals(2L, visibleOptions(dom));
        assertEquals(List.of(), selected);
        popup.close();
        search.value = "beta";
        search.key("keyup", 261);
        assertEquals(List.of(), selected);
    }

    private static long visibleOptions(UiDomControlsTest.Dom dom) {
        return dom.created.stream().filter(node -> !node.removed
                && "popup-option".equals(node.attributes.get("class"))
                && !"display:none".equals(node.attributes.get("style"))).count();
    }
}
