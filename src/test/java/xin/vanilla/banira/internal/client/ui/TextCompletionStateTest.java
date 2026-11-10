package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiTextCompletion;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class TextCompletionStateTest {
    private static UiTextCompletion choice() {
        return new UiTextCompletion(1, 3, "Damage:", "int");
    }

    @Test
    public void preservesSuffixAndPlacesCursorAfterReplacement() {
        assertEquals("{Damage:,display:{}}", choice().apply("{Da,display:{}}"));
        assertEquals(8, choice().cursor());
    }

    @Test
    public void debouncesAndDoesNotPollProviderWhileIdle() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();
        TextCompletionState state = new TextCompletionState((text, cursor) -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(Collections.singletonList(choice()));
        }, clock::get);
        state.update("{Da}", 3, 3, true);
        clock.set(119);
        state.update("{Da}", 3, 3, true);
        assertEquals(0, calls.get());
        clock.set(120);
        state.update("{Da}", 3, 3, true);
        for (int i = 0; i < 10; i++) state.update("{Da}", 3, 3, true);
        assertEquals(1, calls.get());
        assertEquals(1, state.options().size());
    }

    @Test
    public void discardsOldFutureAndNeverReplacesChangedText() {
        AtomicLong clock = new AtomicLong();
        CompletableFuture<java.util.List<UiTextCompletion>> old = new CompletableFuture<>();
        TextCompletionState state = new TextCompletionState((text, cursor) -> old, clock::get);
        state.update("{Da}", 3, 3, true);
        clock.set(120);
        state.update("{Da}", 3, 3, true);
        state.update("{Dis}", 4, 4, true);
        old.complete(Collections.singletonList(choice()));
        state.update("{Dis}", 4, 4, true);
        assertTrue(state.options().isEmpty());
        assertNull(state.choose(0, "{Da}", 3));
    }

    @Test
    public void selectedTextAndUnfocusedFieldsDoNotRequestSuggestions() {
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> {
            throw new AssertionError("Inactive provider");
        }, clock::get);
        state.update("{Da}", 3, 1, true);
        clock.set(1000);
        state.update("{Da}", 3, 1, true);
        state.update("{Da}", 3, 3, false);
        assertTrue(state.options().isEmpty());
    }

    @Test
    public void failuresAndTimeoutsLeaveManualInputAvailable() {
        AtomicLong clock = new AtomicLong();
        CompletableFuture<java.util.List<UiTextCompletion>> future = new CompletableFuture<>();
        TextCompletionState state = new TextCompletionState((text, cursor) -> future, clock::get);
        state.update("{Da}", 3, 3, true);
        clock.set(120);
        state.update("{Da}", 3, 3, true);
        clock.set(2001);
        state.update("{Da}", 3, 3, true);
        future.complete(Collections.singletonList(choice()));
        state.update("{Da}", 3, 3, true);
        assertTrue(state.options().isEmpty());
        state.clear();
    }

    @Test
    public void dismissDoesNotReopenUntilTextOrCursorChanges() {
        AtomicLong clock = new AtomicLong();
        TextCompletionState state = new TextCompletionState((text, cursor) -> CompletableFuture.completedFuture(Collections.singletonList(choice())), clock::get);
        state.update("{Da}", 3, 3, true);
        clock.set(120);
        state.update("{Da}", 3, 3, true);
        state.dismiss();
        clock.set(1000);
        state.update("{Da}", 3, 3, true);
        assertTrue(state.options().isEmpty());
    }
}
