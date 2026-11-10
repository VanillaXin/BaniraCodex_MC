package com.mt1006.nbt_ac.autocomplete;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public final class NbtSuggestionManager {
    public static String input, name;
    public static boolean fail;

    public static CompletableFuture<Suggestions> loadFromName(String root, String text, SuggestionsBuilder builder, boolean path) {
        input = text;
        name = root;
        if (fail) throw new IllegalStateException("Unavailable legacy suggestions");
        return builder.createOffset(1).suggest("Damage:").buildFuture();
    }
}
