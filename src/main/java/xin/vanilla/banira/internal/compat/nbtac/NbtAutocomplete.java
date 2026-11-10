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
 * Optional public entry points only: no upstream classes are linked into server or UI signatures.
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
            return (text, cursor) -> request(method, name, text, cursor, false);
        } catch (ReflectiveOperationException | LinkageError | SecurityException unavailable) {
            return bindLegacy(loader, name);
        }
    }

    private static UiTextCompletionProvider bindLegacy(ClassLoader loader, String name) {
        try {
            Class<?> manager = Class.forName("com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager", false, loader);
            Method method = manager.getMethod("loadFromName", String.class, String.class, SuggestionsBuilder.class, boolean.class);
            if (!Modifier.isStatic(method.getModifiers()) || !CompletableFuture.class.isAssignableFrom(method.getReturnType()))
                return null;
            return (text, cursor) -> request(method, name, text, cursor, true);
        } catch (ReflectiveOperationException | LinkageError | SecurityException unavailable) {
            return null;
        }
    }

    private static CompletableFuture<List<UiTextCompletion>> request(Method method, String name, String text, int cursor, boolean legacy) {
        if (cursor < 0 || cursor > text.length()) return CompletableFuture.completedFuture(Collections.emptyList());
        try {
            String prefix = text.substring(0, cursor);
            SuggestionsBuilder builder = new SuggestionsBuilder(prefix, 0);
            Object result = legacy ? method.invoke(null, name, prefix, builder, false)
                    : method.invoke(null, prefix, name, builder, false, null);
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
