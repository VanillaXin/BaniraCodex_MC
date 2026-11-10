package com.mt1006.nbt_ac.autocomplete;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public final class NbtSuggestionManager {
    public static int preparations;

    public static void clearIfNeeded(SuggestionsBuilder builder) {
        preparations++;
    }

    public static CompletableFuture<Suggestions> finishSuggestions(SuggestionList list, SuggestionsBuilder builder,
                                                                   CustomTagParser.Suggestion suggestion, int cursor) {
        SuggestionsBuilder offset = builder.createOffset(cursor);
        for (String text : list) offset.suggest(text);
        return offset.buildFuture();
    }

    public static CompletableFuture<Suggestions> loadFromName(String name, String text, SuggestionsBuilder builder, boolean path) {
        throw new AssertionError("Legacy item-root NBT must not be used for custom_data");
    }
}
