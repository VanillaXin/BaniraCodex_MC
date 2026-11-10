package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiTextCompletion;
import xin.vanilla.banira.api.client.ui.UiTextCompletionProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;

/**
 * Polls on the UI thread; provider futures never mutate a live document.
 */
public final class TextCompletionState {
    private final UiTextCompletionProvider provider;
    private final LongSupplier clock;
    private String input;
    private int cursor;
    private long changedAt;
    private CompletableFuture<List<UiTextCompletion>> pending;
    private List<UiTextCompletion> options = Collections.emptyList();
    private int selected;
    private boolean requested;

    public TextCompletionState(UiTextCompletionProvider provider) {
        this(provider, System::currentTimeMillis);
    }

    public TextCompletionState(UiTextCompletionProvider provider, LongSupplier clock) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void update(String text, int cursor, int selection, boolean active) {
        if (!active || cursor != selection || cursor < 0 || cursor > text.length()) {
            clear();
            return;
        }
        long now = clock.getAsLong();
        if (!Objects.equals(input, text) || this.cursor != cursor) {
            input = text;
            this.cursor = cursor;
            changedAt = now;
            pending = null;
            options = Collections.emptyList();
            selected = 0;
            requested = false;
        }
        if (!requested && now - changedAt >= 120) {
            requested = true;
            try {
                pending = provider.complete(text, cursor);
            } catch (RuntimeException | LinkageError unavailable) {
                pending = null;
            }
        }
        if (pending != null && pending.isDone()) {
            try {
                List<UiTextCompletion> result = pending.getNow(Collections.emptyList());
                List<UiTextCompletion> accepted = new ArrayList<>();
                if (result != null) for (UiTextCompletion option : result) {
                    if (option != null && option.end() <= cursor && !option.apply(text).equals(text)) {
                        accepted.add(option);
                        if (accepted.size() == 64) break;
                    }
                }
                options = Collections.unmodifiableList(accepted);
            } catch (RuntimeException | LinkageError unavailable) {
                options = Collections.emptyList();
            }
            pending = null;
        } else if (pending != null && now - changedAt > 2000) pending = null;
    }

    public List<UiTextCompletion> options() {
        return options;
    }

    public int selected() {
        return selected;
    }

    public void move(int delta) {
        if (!options.isEmpty()) selected = Math.floorMod(selected + delta, options.size());
    }

    public UiTextCompletion choose(int index, String text, int cursor) {
        if (!Objects.equals(input, text) || this.cursor != cursor || index < 0 || index >= options.size()) return null;
        UiTextCompletion option = options.get(index);
        dismiss();
        return option;
    }

    public void dismiss() {
        options = Collections.emptyList();
        pending = null;
        requested = true;
    }

    public void clear() {
        dismiss();
        input = null;
    }
}
