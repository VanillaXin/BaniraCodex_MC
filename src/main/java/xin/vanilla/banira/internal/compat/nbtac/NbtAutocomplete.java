package xin.vanilla.banira.internal.compat.nbtac;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import xin.vanilla.banira.api.client.ui.UiTextCompletion;
import xin.vanilla.banira.api.client.ui.UiTextCompletionProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Optional public API only: no upstream classes are linked into server or UI signatures.
 */
public final class NbtAutocomplete {
    private NbtAutocomplete() {
    }

    public static UiTextCompletionProvider item(String itemId) {
        return bind(NbtAutocomplete.class.getClassLoader(), "item/minecraft:custom_data", itemId);
    }

    static UiTextCompletionProvider bind(ClassLoader loader, String name) {
        return bind(loader, name, null);
    }

    static UiTextCompletionProvider bind(ClassLoader loader, String name, String itemId) {
        try {
            Class<?> api = Class.forName("net.mt1006.nbtac.api.NBTacAPI", false, loader);
            Method method = itemDataMethod(api);
            return (text, cursor) -> request(method, null, name, itemId, text, cursor);
        } catch (ReflectiveOperationException | LinkageError | SecurityException unavailable) {
            try {
                LegacyComponentApi legacy = LegacyComponentApi.bind(loader);
                return (text, cursor) -> request(null, legacy, name, itemId, text, cursor);
            } catch (ReflectiveOperationException | LinkageError | SecurityException unsupported) {
                return null;
            }
        }
    }

    static Method itemDataMethod(Class<?> api) throws NoSuchMethodException {
        for (Method method : api.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals("getItemDataSuggestions") && Modifier.isStatic(method.getModifiers())
                    && CompletableFuture.class.isAssignableFrom(method.getReturnType()) && parameters.length == 5
                    && parameters[0] == String.class && parameters[1] == String.class && !parameters[2].isPrimitive()
                    && parameters[3] == SuggestionsBuilder.class && parameters[4] == Function.class) return method;
        }
        throw new NoSuchMethodException("NBTac item data suggestions unavailable");
    }

    private static CompletableFuture<List<UiTextCompletion>> request(Method method, LegacyComponentApi legacy,
                                                                     String name, String itemId, String text, int cursor) {
        if (cursor < 0 || cursor > text.length()) return CompletableFuture.completedFuture(Collections.emptyList());
        try {
            String prefix = text.substring(0, cursor);
            SuggestionsBuilder builder = new SuggestionsBuilder(prefix, 0);
            Object result = method != null ? method.invoke(null, prefix, name, null, builder, null)
                    : legacy.suggest(name, itemId, prefix, builder);
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

    private record LegacyComponentApi(Field ready, Field components, Class<?> componentType,
                                      Constructor<?> listConstructor,
                                      Constructor<?> parserConstructor, Object parserType, Method read,
                                      Method cursor, Method prepare, Method finish) {
        static LegacyComponentApi bind(ClassLoader loader) throws ReflectiveOperationException {
            String root = "com.mt1006.nbt_ac.autocomplete.";
            Class<?> list = Class.forName(root + "SuggestionList", false, loader);
            Class<?> component = Class.forName(root + "suggestions.NbtSuggestion", false, loader);
            Class<?> parser = Class.forName(root + "CustomTagParser", false, loader);
            Class<?> type = Class.forName(root + "CustomTagParser$Type", false, loader);
            Class<?> suggestion = Class.forName(root + "CustomTagParser$Suggestion", false, loader);
            Class<?> manager = Class.forName(root + "NbtSuggestionManager", false, loader);
            Class<?> componentManager = Class.forName(root + "DataComponentManager", false, loader);
            Field ready = Class.forName(root + "loader.Loader", false, loader).getField("finished");
            Field components = componentManager.getField("componentMap");
            Method read = parser.getMethod("read", list, component, String.class);
            Method cursor = parser.getMethod("getCursor");
            Method finish = manager.getMethod("finishSuggestions", list, SuggestionsBuilder.class, suggestion, int.class);
            Method prepare = manager.getMethod("clearIfNeeded", SuggestionsBuilder.class);
            if (!Modifier.isStatic(ready.getModifiers()) || ready.getType() != boolean.class
                    || !Modifier.isStatic(components.getModifiers()) || !Map.class.isAssignableFrom(components.getType())
                    || Modifier.isStatic(read.getModifiers()) || read.getReturnType() != suggestion
                    || Modifier.isStatic(cursor.getModifiers()) || cursor.getReturnType() != int.class
                    || !Modifier.isStatic(finish.getModifiers())
                    || !Modifier.isStatic(prepare.getModifiers()) || prepare.getReturnType() != void.class
                    || !CompletableFuture.class.isAssignableFrom(finish.getReturnType()) || !type.isEnum()) {
                throw new NoSuchMethodException("NBTac component parser unavailable");
            }
            return new LegacyComponentApi(ready, components, component, list.getConstructor(),
                    parser.getConstructor(String.class, type), type.getField("COMPONENT").get(null), read, cursor, prepare, finish);
        }

        Object suggest(String name, String itemId, String prefix, SuggestionsBuilder builder) throws ReflectiveOperationException {
            if (!ready.getBoolean(null)) return builder.buildFuture();
            Object entries = components.get(null);
            Object component = entries instanceof Map<?, ?> map ? map.get(name) : null;
            if (!componentType.isInstance(component)) return builder.buildFuture();
            prepare.invoke(null, builder);
            // Match the upstream item-component parser; never fall back to legacy item-root NBT.
            Object list = listConstructor.newInstance();
            Object parser = parserConstructor.newInstance(prefix, parserType);
            Object suggestion = read.invoke(parser, list, component, itemId);
            return finish.invoke(null, list, builder, suggestion, (int) cursor.invoke(parser));
        }
    }
}
