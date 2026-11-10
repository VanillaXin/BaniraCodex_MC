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
    public void mouseCompletionPreservesSuffixAndClosesPopup() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        dom.ids.get("body").rectangle = new xin.vanilla.banira.client.data.ScreenCoordinate(0, 0, 800, 600);
        var input = dom.ids.get("text");
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
        var button = dom.created.stream().filter(n -> n.text.equals("Damage:")).findFirst().orElseThrow();
        button.emit("mousedown", 0);
        assertEquals("{Damage:,display:{}}", input.value);
        assertEquals(input.value, draft.get());
        assertEquals(8, input.end);
        assertTrue(button.removed);
        popup.close();
    }

    @Test
    public void blurAndCloseDiscardPendingResults() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        var input = dom.ids.get("text");
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
        var input = dom.ids.get("text");
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
        var input = dom.ids.get("text");
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
    public void oversizedCompletionLeavesTheDraftUntouched() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        var input = dom.ids.get("text");
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
