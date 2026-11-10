package net.mt1006.nbtac.api;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Test double for the optional public API; never included in published archives.
 */
public final class NBTacAPI {
    public static String input, name;
    public static boolean fail;

    public static CompletableFuture<Suggestions> getNbtSuggestions(String text, String root, SuggestionsBuilder builder,
                                                                   boolean path, Function<?, ?> process) {
        input = text;
        name = root;
        if (fail) throw new IllegalStateException("Unavailable suggestion data");
        return builder.createOffset(1).suggest("Damage:").buildFuture();
    }
}
