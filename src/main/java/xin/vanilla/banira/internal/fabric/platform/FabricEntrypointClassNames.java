package xin.vanilla.banira.internal.fabric.platform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从 Fabric 元数据提取普通类入口；方法入口不能用于主类反查。
 */
final class FabricEntrypointClassNames {
    private FabricEntrypointClassNames() {
    }

    static List<String> read(JsonObject metadata, List<String> keys) {
        Set<String> names = new LinkedHashSet<>();
        if (metadata == null || keys == null || !metadata.has("entrypoints")
                || !metadata.get("entrypoints").isJsonObject()) {
            return new ArrayList<>();
        }
        JsonObject entrypoints = metadata.getAsJsonObject("entrypoints");
        for (String key : keys) {
            JsonElement entries = entrypoints.get(key);
            if (entries == null || entries.isJsonNull()) continue;
            if (entries.isJsonArray()) {
                JsonArray array = entries.getAsJsonArray();
                for (JsonElement entry : array) addClassName(entry, names);
            } else {
                addClassName(entries, names);
            }
        }
        return new ArrayList<>(names);
    }

    private static void addClassName(JsonElement entry, Set<String> names) {
        String value = null;
        if (entry != null && entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
            value = entry.getAsString();
        } else if (entry != null && entry.isJsonObject()) {
            JsonElement objectValue = entry.getAsJsonObject().get("value");
            if (objectValue != null && objectValue.isJsonPrimitive()
                    && objectValue.getAsJsonPrimitive().isString()) {
                value = objectValue.getAsString();
            }
        }
        if (value == null) return;
        value = value.trim();
        if (!value.isEmpty() && !value.contains("::")) names.add(value);
    }
}
