package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiTextCompletion;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class UiCompletionPopupTest {
    @Test
    public void strictDomKeepsTextureAndInputAttributeBoundariesSeparate() throws Exception {
        UiDomControlsTest.assertActualAttributeGuard();
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        UiDomAccess.Node input = (UiDomAccess.Node) dom.ids.get("text").proxy;
        assertThrows(IllegalArgumentException.class, () -> input.attribute("tabindex", "-1"));
        assertThrows(IllegalArgumentException.class, () -> input.attribute("src", "minecraft:textures/item/apple.png"));
        UiDomAccess.Node texture = (UiDomAccess.Node) dom.new Node("texture").proxy;
        texture.attribute("src", "minecraft:textures/item/apple.png");
        assertThrows(IllegalArgumentException.class, () -> texture.attribute("src", "not a resource location"));
    }

    @Test
    public void completionUsesOnlyAttributesAcceptedByTheRealAuiAdapter() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        dom.rejectUnsupportedAttributes = true;
        lombok.var input = dom.ids.get("text");
        input.value = "{Da}";
        input.start = input.end = 3;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(
                Collections.singletonList(new UiTextCompletion(1, 3, "Damage:", "int"))), clock::get);
        UiCompletionPopup popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000,
                text -> {
                });
        try {
            popup.update();
            clock.set(120);
            popup.update();
            assertTrue(dom.created.stream().anyMatch(node -> node.text.equals("Damage:")));
        } finally {
            popup.close();
        }
    }

    @Test
    public void mouseCompletionPreservesSuffixAndClosesPopup() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        dom.ids.get("body").rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 800, 600);
        lombok.var input = dom.ids.get("text");
        input.rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(10, 40, 200, 20);
        input.value = "{Da,display:{}}";
        input.start = input.end = 3;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(
                Collections.singletonList(new UiTextCompletion(1, 3, "Damage:", "int"))), clock::get);
        AtomicReference<String> draft = new AtomicReference<>();
        UiCompletionPopup popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000, draft::set);
        popup.update();
        clock.set(120);
        popup.update();
        lombok.var button = dom.created.stream().filter(n -> n.text.equals("Damage:")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        // AUI changes focus before dispatching mousedown to the candidate.
        input.focused = false;
        input.emit("blur");
        button.emit("mousedown", 0);
        assertEquals("{Damage:,display:{}}", input.value);
        assertEquals(input.value, draft.get());
        assertEquals(8, input.end);
        assertTrue(input.focused);
        assertTrue(button.removed);
        button.emit("mousedown", 0);
        assertEquals("{Damage:,display:{}}", draft.get());
        popup.close();
    }

    @Test
    public void blurAndCloseDiscardPendingResults() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        lombok.var input = dom.ids.get("text");
        input.value = "{Da}";
        input.start = input.end = 3;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        CompletableFuture<java.util.List<UiTextCompletion>> future = new CompletableFuture<>();
        TextCompletionState state = new TextCompletionState((text, cursor) -> future, clock::get);
        UiCompletionPopup popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000,
                text -> fail("Closed field callback"));
        popup.update();
        clock.set(120);
        popup.update();
        input.focused = false;
        input.emit("blur");
        popup.close();
        future.complete(Collections.singletonList(new UiTextCompletion(1, 3, "Damage:", "")));
        popup.update();
        assertTrue(dom.created.isEmpty());
    }

    @Test
    public void keyboardNavigationAndTabApplyExactlyOnce() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        dom.ids.get("body").rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 800, 600);
        lombok.var input = dom.ids.get("text");
        input.rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(10, 40, 200, 20);
        input.value = "{D}";
        input.start = input.end = 2;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(java.util.Arrays.asList(
                new UiTextCompletion(1, 2, "Damage:", "int"), new UiTextCompletion(1, 2, "display:", "compound"))), clock::get);
        java.util.concurrent.atomic.AtomicInteger changes = new java.util.concurrent.atomic.AtomicInteger();
        UiCompletionPopup popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000,
                text -> changes.incrementAndGet());
        popup.update();
        clock.set(120);
        popup.update();
        lombok.var first = dom.created.stream().filter(n -> n.text.equals("Damage:")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        lombok.var second = dom.created.stream().filter(n -> n.text.equals("display:")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
        assertEquals("true", first.attributes.get("aria-selected"));
        assertEquals("false", second.attributes.get("aria-selected"));
        key(input, 264);
        assertEquals("false", first.attributes.get("aria-selected"));
        assertEquals("true", second.attributes.get("aria-selected"));
        key(input, 265);
        assertEquals("true", first.attributes.get("aria-selected"));
        key(input, 264);
        key(input, 258);
        assertEquals("{display:}", input.value);
        assertEquals(1, changes.get());
        assertEquals(9, input.end);
        popup.close();
    }

    @Test
    public void unchangedFramesDoNotRewriteCandidatesOrLayout() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        dom.ids.get("body").rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 800, 600);
        lombok.var input = dom.ids.get("text");
        input.rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(10, 40, 200, 20);
        input.value = "{D}";
        input.start = input.end = 2;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(
                Collections.singletonList(new UiTextCompletion(1, 2, "Damage:", "int"))), clock::get);
        UiCompletionPopup popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000, text -> {
        });
        popup.update();
        clock.set(120);
        popup.update();
        int writes = dom.created.stream().mapToInt(node -> node.writes).sum();
        for (int index = 0; index < 20; index++) popup.update();
        assertEquals(writes, dom.created.stream().mapToInt(node -> node.writes).sum());
        popup.close();
    }

    @Test
    public void outsideBlurDisposesVisibleRowsOnTheNextLayout() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var input = dom.ids.get("text");
        input.value = "{D}";
        input.start = input.end = 2;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        lombok.var state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(
                Collections.singletonList(new UiTextCompletion(1, 2, "Damage:", "int"))), clock::get);
        try (lombok.var popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000,
                text -> fail("Outside click must not complete"))) {
            popup.update();
            clock.set(120);
            popup.update();
            lombok.var button = dom.created.stream().filter(n -> n.text.equals("Damage:")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
            input.focused = false;
            input.emit("blur");
            popup.update();
            assertTrue(button.removed);
            button.emit("mousedown", 0);
            assertEquals("{D}", input.value);
            assertFalse(input.focused);
            assertTrue(state.options().isEmpty());
        }
    }

    @Test
    public void staleDraftAndNonPrimaryMouseCannotApplyACandidate() throws Exception {
        lombok.var dom = new UiDomControlsTest.Dom();
        lombok.var input = dom.ids.get("text");
        input.value = "{D}";
        input.start = input.end = 2;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        lombok.var state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(
                Collections.singletonList(new UiTextCompletion(1, 2, "Damage:", "int"))), clock::get);
        try (lombok.var popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 1000,
                text -> fail("Stale or secondary click must not complete"))) {
            popup.update();
            clock.set(120);
            popup.update();
            lombok.var button = dom.created.stream().filter(n -> n.text.equals("Damage:")).findFirst().orElseThrow(java.util.NoSuchElementException::new);
            button.emit("mousedown", 1);
            assertFalse(button.removed);
            assertEquals("{D}", input.value);
            input.value = "{Da}";
            input.start = input.end = 3;
            input.focused = false;
            input.emit("blur");
            button.emit("mousedown", 0);
            assertEquals("{Da}", input.value);
            assertFalse(input.focused);
            assertTrue(button.removed);
        }
    }

    @Test
    public void oversizedCompletionLeavesTheDraftUntouched() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        lombok.var input = dom.ids.get("text");
        input.value = "{D}";
        input.start = input.end = 2;
        input.focused = true;
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(
                Collections.singletonList(new UiTextCompletion(1, 2, "Damage:", "int"))), clock::get);
        UiCompletionPopup popup = new UiCompletionPopup((UiDomAccess) dom.access, (UiDomAccess.Node) input.proxy, state, 3,
                text -> fail("Oversized completion must not change draft"));
        popup.update();
        clock.set(120);
        popup.update();
        key(input, 257);
        assertEquals("{D}", input.value);
        popup.close();
    }

    private static void key(UiDomControlsTest.Dom.Node input, int key) {
        java.util.concurrent.atomic.AtomicBoolean consumed = new java.util.concurrent.atomic.AtomicBoolean();
        UiDomAccess.Event event = new UiDomAccess.Event() {
            public int keyCode() {
                return key;
            }

            public void consume() {
                consumed.set(true);
            }
        };
        for (java.util.function.Consumer<Object> action : new java.util.ArrayList<>(input.events.get("keydown")))
            action.accept(event);
        assertTrue(consumed.get());
    }
}
