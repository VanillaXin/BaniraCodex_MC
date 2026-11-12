package xin.vanilla.banira.internal.compat.nbtac;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import xin.vanilla.banira.api.client.ui.UiTextCompletion;
import xin.vanilla.banira.api.client.ui.UiTextCompletionProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class NbtAutocomplete {
    private NbtAutocomplete() {
    }

    public static UiTextCompletionProvider item(String itemId) {
        return (text, cursor) -> request(itemId, text, cursor);
    }

    private static CompletableFuture<List<UiTextCompletion>> request(String itemId, String text, int cursor) {
        if (text == null || cursor < 0 || cursor > text.length() || Minecraft.getInstance().level == null)
            return CompletableFuture.completedFuture(Collections.emptyList());
        try {
            // Use the same parser as /give; NBTac augments it when installed.
            String head = itemId + (text.stripLeading().startsWith("{") ? "[minecraft:custom_data=" : "");
            String input = head + text.substring(0, cursor);
            return new ItemParser(Minecraft.getInstance().level.registryAccess())
                    .fillSuggestions(new SuggestionsBuilder(input, 0)).handle((value, failure) -> {
                        if (failure != null || value == null) return Collections.emptyList();
                        List<UiTextCompletion> options = new ArrayList<>();
                        for (Suggestion suggestion : value.getList()) {
                            int start = suggestion.getRange().getStart() - head.length();
                            int end = suggestion.getRange().getEnd() - head.length();
                            if (start < 0 || end < start || end > cursor) continue;
                            options.add(new UiTextCompletion(start, end, suggestion.getText(),
                                    suggestion.getTooltip() == null ? "" : suggestion.getTooltip().getString()));
                            if (options.size() == 64) break;
                        }
                        return options;
                    });
        } catch (LinkageError | RuntimeException unavailable) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
}
