package xin.vanilla.banira.internal.compat.nbtac;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import xin.vanilla.banira.api.client.ui.UiTextCompletion;
import xin.vanilla.banira.api.client.ui.UiTextCompletionProvider;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Optional public API only: no upstream classes are linked into server or UI signatures.
 */
public final class NbtAutocomplete {
    private NbtAutocomplete() {
    }

    public static UiTextCompletionProvider item(String itemId) {
        return bind(NbtAutocomplete.class.getClassLoader(), "item/" + itemId);
    }

    static UiTextCompletionProvider bind(ClassLoader loader, String name) {
        try {
            Class<?> api = Class.forName("net.mt1006.nbtac.api.NBTacAPI", false, loader);
            Method method = api.getMethod("getNbtSuggestions", String.class, String.class,
                    SuggestionsBuilder.class, boolean.class, Function.class);
            if (!Modifier.isStatic(method.getModifiers()) || !CompletableFuture.class.isAssignableFrom(method.getReturnType()))
                return null;
            return (text, cursor) -> request(method, name, text, cursor);
        } catch (ReflectiveOperationException | LinkageError | SecurityException unavailable) {
            return null;
        }
    }

    private static CompletableFuture<List<UiTextCompletion>> request(Method method, String name, String text, int cursor) {
        if (cursor < 0 || cursor > text.length()) return CompletableFuture.completedFuture(Collections.emptyList());
        try {
            String prefix = text.substring(0, cursor);
            Object result = method.invoke(null, prefix, name, new SuggestionsBuilder(prefix, 0), false, null);
            if (!(result instanceof CompletableFuture<?>))
                return CompletableFuture.completedFuture(Collections.emptyList());
            return ((CompletableFuture<?>) result).handle((value, failure) -> {
                if (failure != null || !(value instanceof Suggestions)) return Collections.emptyList();
                List<UiTextCompletion> options = new ArrayList<>();
                for (Suggestion suggestion : ((Suggestions) value).getList()) {
                    int start = suggestion.getRange().getStart(), end = suggestion.getRange().getEnd();
                    if (start < 0 || end < start || end > cursor) continue;
                    options.add(new UiTextCompletion(start, end, suggestion.getText(),
                            suggestion.getTooltip() == null ? "" : suggestion.getTooltip().getString()));
                    if (options.size() == 64) break;
                }
                return options;
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }
}
