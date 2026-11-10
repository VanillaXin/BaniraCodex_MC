package xin.vanilla.banira.api.client.ui;

import java.util.Objects;

public final class UiTextCompletion {
    private final int start;
    private final int end;
    private final String text;
    private final String tooltip;

    public UiTextCompletion(int start, int end, String text, String tooltip) {
        if (start < 0 || end < start) throw new IllegalArgumentException("Invalid completion range");
        this.start = start;
        this.end = end;
        this.text = Objects.requireNonNull(text, "text");
        this.tooltip = tooltip == null ? "" : tooltip;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public String text() {
        return text;
    }

    public String tooltip() {
        return tooltip;
    }

    public String apply(String input) {
        if (end > input.length()) throw new IllegalArgumentException("Completion outside input");
        return input.substring(0, start) + text + input.substring(end);
    }

    public int cursor() {
        return start + text.length();
    }
}
