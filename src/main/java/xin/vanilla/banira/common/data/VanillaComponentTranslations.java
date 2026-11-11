package xin.vanilla.banira.common.data;

import com.google.gson.*;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

final class VanillaComponentTranslations {
    private VanillaComponentTranslations() {
    }

    private static final class VanillaKeys {
        private static final Set<String> VALUES = load();

        private static Set<String> load() {
            // Classpath Minecraft resources are independent of the client's merged language table.
            try (InputStream stream = Component.openVanillaLanguage()) {
                if (stream == null) return Collections.emptySet();
                JsonObject language = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                Set<String> keys = new HashSet<>();
                for (Map.Entry<String, JsonElement> entry : language.entrySet()) keys.add(entry.getKey());
                return Collections.unmodifiableSet(keys);
            } catch (java.io.IOException | RuntimeException unavailable) {
                return Collections.emptySet();
            }
        }
    }

    static boolean isVanillaKey(String key) {
        return VanillaKeys.VALUES.contains(key);
    }

    static JsonElement prepare(JsonElement input, String language, boolean vanillaOnly,
                               BiFunction<String, JsonArray, JsonElement> format) {
        if (input == null || input.isJsonNull() || input.isJsonPrimitive()) return input;
        if (input.isJsonArray()) {
            JsonArray output = new JsonArray();
            for (JsonElement element : input.getAsJsonArray())
                output.add(prepare(element, language, vanillaOnly, format));
            return output;
        }
        JsonObject source = input.getAsJsonObject();
        JsonObject output = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String name = entry.getKey();
            if ("extra".equals(name) || "with".equals(name) || "separator".equals(name)) {
                output.add(name, prepare(entry.getValue(), language, vanillaOnly, format));
            } else if ("hoverEvent".equals(name) || "hover_event".equals(name)) {
                output.add(name, hover(entry.getValue(), language, vanillaOnly, format));
            } else if (!vanillaOnly || !"font".equals(name) || !foreignFont(entry.getValue())) {
                output.add(name, copy(entry.getValue()));
            }
        }
        if (source.has("translate") && !isVanillaKey(source.get("translate").getAsString())) {
            String key = source.get("translate").getAsString();
            String template = serverTemplate(key, language);
            if (template == null) template = source.has("fallback") ? source.get("fallback").getAsString() : key;
            JsonArray arguments = output.has("with") ? output.getAsJsonArray("with") : new JsonArray();
            return replaceContent(output, format.apply(template, arguments));
        }
        if (vanillaOnly && source.has("keybind") && !isVanillaKey(source.get("keybind").getAsString())) {
            String key = source.get("keybind").getAsString();
            String translated = serverTemplate(key, language);
            return replaceContent(output, new JsonPrimitive(translated == null ? key : translated));
        }
        return output;
    }

    static String noArgumentTemplate(String template) {
        for (int index = 0; index < template.length(); index++) {
            if (template.charAt(index) == '%') {
                if (index + 1 >= template.length() || template.charAt(index + 1) != '%') return template;
                index++;
            }
        }
        return template.replace("%%", "%");
    }

    private static boolean foreignFont(JsonElement font) {
        String name = font.getAsString();
        return name.indexOf(':') >= 0 && !name.startsWith("minecraft:");
    }

    private static String serverTemplate(String key, String language) {
        String text = Translator.of(Banira.MOD_ID).getTranslation(key, language);
        if (!key.equals(text)) return text;
        if (BaniraPlatforms.isInstalled()) {
            for (String part : key.split("[.:]")) {
                if (!"minecraft".equals(part) && BaniraPlatforms.get().isModLoaded(part)) {
                    text = Translator.of(part).getTranslation(key, language);
                    if (!key.equals(text)) return text;
                }
            }
        }
        return null;
    }

    private static JsonObject replaceContent(JsonObject source, JsonElement formatted) {
        JsonObject result;
        if (formatted.isJsonObject()) {
            result = formatted.getAsJsonObject();
        } else {
            result = new JsonObject();
            if (formatted.isJsonArray()) {
                result.addProperty("text", "");
                result.add("extra", formatted);
            } else {
                result.addProperty("text", formatted.isJsonNull() ? "" : formatted.getAsString());
            }
        }
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String name = entry.getKey();
            if (!"translate".equals(name) && !"fallback".equals(name) && !"with".equals(name)
                    && !"keybind".equals(name) && !"extra".equals(name)) result.add(name, entry.getValue());
        }
        if (source.has("extra")) {
            JsonArray children = result.has("extra") ? result.getAsJsonArray("extra") : new JsonArray();
            for (JsonElement child : source.getAsJsonArray("extra")) children.add(child);
            result.add("extra", children);
        }
        return result;
    }

    private static JsonElement hover(JsonElement value, String language, boolean vanillaOnly,
                                     BiFunction<String, JsonArray, JsonElement> format) {
        JsonElement copied = copy(value);
        if (!copied.isJsonObject()) return copied;
        JsonObject event = copied.getAsJsonObject();
        if (!event.has("action")) return event;
        String action = event.get("action").getAsString();
        for (String field : new String[]{"contents", "value"}) {
            if (!event.has(field)) continue;
            if ("show_text".equals(action)) {
                event.add(field, prepare(event.get(field), language, vanillaOnly, format));
            } else if ("show_entity".equals(action) && event.get(field).isJsonObject()) {
                JsonObject entity = event.getAsJsonObject(field);
                if (entity.has("name")) entity.add("name", prepare(entity.get("name"), language, vanillaOnly, format));
            } else if ("show_item".equals(action) && event.get(field).isJsonObject()) {
                JsonObject item = event.getAsJsonObject(field);
                if (item.has("components") && item.get("components").isJsonObject()) {
                    JsonObject components = item.getAsJsonObject("components");
                    for (String name : new String[]{"minecraft:custom_name", "minecraft:item_name", "minecraft:lore"})
                        if (components.has(name))
                            components.add(name, prepare(components.get(name), language, vanillaOnly, format));
                }
                if (item.has("tag") && item.get("tag").isJsonPrimitive())
                    item.addProperty("tag", Component.prepareItemHoverTag(item.get("tag").getAsString(), language, vanillaOnly));
            }
        }
        return event;
    }

    private static JsonElement copy(JsonElement input) {
        if (input == null || input.isJsonNull() || input.isJsonPrimitive()) return input;
        if (input.isJsonArray()) {
            JsonArray output = new JsonArray();
            for (JsonElement element : input.getAsJsonArray()) output.add(copy(element));
            return output;
        }
        JsonObject output = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : input.getAsJsonObject().entrySet())
            output.add(entry.getKey(), copy(entry.getValue()));
        return output;
    }
}
