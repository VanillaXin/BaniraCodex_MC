package com.mt1006.nbt_ac.autocomplete;

import com.mt1006.nbt_ac.autocomplete.suggestions.NbtSuggestion;

public final class CustomTagParser {
    public enum Type {COMPOUND, COMPONENT}

    public enum Suggestion {TAG}

    public static String input;
    public static String itemId;
    public static Type mode;
    public static boolean fail;
    public static int calls;

    public CustomTagParser(String text, Type type) {
        input = text;
        mode = type;
    }

    public Suggestion read(SuggestionList list, NbtSuggestion component, String parent) {
        calls++;
        if (fail) throw new IllegalStateException("fixture failure");
        itemId = parent;
        list.add(component.label);
        return Suggestion.TAG;
    }

    public int getCursor() {
        return 1;
    }
}
