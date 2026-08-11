package xin.vanilla.banira.internal.client;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;

import java.util.*;

public final class ConfigEditorState {
    private final ConfigHolder holder;
    private final Map<String, Object> modifiedValues = new LinkedHashMap<>();
    @Getter
    @Accessors(fluent = true)
    private final Map<String, ConfigEditorEntryWidget> entryWidgets = new LinkedHashMap<>();
    private final Set<String> syncTouchedPaths = new LinkedHashSet<>();
    private final Map<String, Object> baselineValues = new LinkedHashMap<>();

    public ConfigEditorState(ConfigHolder holder) {
        this.holder = holder;
    }

    public void clearEntries() {
        entryWidgets.clear();
        baselineValues.clear();
        syncTouchedPaths.clear();
    }

    public void clearPendingChanges() {
        modifiedValues.clear();
        syncTouchedPaths.clear();
    }

    public void registerEntry(String path, ConfigEditorEntryWidget widget) {
        if (path != null && widget != null) {
            entryWidgets.put(path, widget);
            baselineValues.put(path, snapshot(widget.getValue()));
        }
    }

    public void markModified(String path, Object value) {
        if (path != null) {
            if (Objects.deepEquals(baselineValues.get(path), value)) {
                modifiedValues.remove(path);
                syncTouchedPaths.remove(path);
            } else {
                modifiedValues.put(path, value);
                syncTouchedPaths.add(path);
            }
        }
    }

    public void collectModifiedFromWidgets() {
        modifiedValues.clear();
        for (Map.Entry<String, ConfigEditorEntryWidget> e : entryWidgets.entrySet()) {
            if (!e.getValue().isValid()) {
                continue;
            }
            Object v = e.getValue().getValue();
            if (v != null && !Objects.deepEquals(v, baselineValues.get(e.getKey()))) {
                modifiedValues.put(e.getKey(), v);
            }
        }
    }

    /**
     * 返回相对打开界面时基线的真实改动项数；无效但已操作的输入同样计入。
     */
    public int pendingChangeCount() {
        Set<String> paths = new LinkedHashSet<>(syncTouchedPaths);
        for (Map.Entry<String, ConfigEditorEntryWidget> entry : entryWidgets.entrySet()) {
            Object value = entry.getValue().getValue();
            if (!entry.getValue().isValid()) {
                continue;
            }
            if (value != null && !Objects.deepEquals(value, baselineValues.get(entry.getKey()))) {
                paths.add(entry.getKey());
            } else {
                paths.remove(entry.getKey());
            }
        }
        return paths.size();
    }

    public void markClean() {
        clearPendingChanges();
        for (Map.Entry<String, ConfigEditorEntryWidget> entry : entryWidgets.entrySet()) {
            baselineValues.put(entry.getKey(), snapshot(entry.getValue().getValue()));
        }
    }

    public void applyModifiedToHolder() {
        for (Map.Entry<String, Object> e : modifiedValues.entrySet()) {
            holder.set(e.getKey(), e.getValue());
        }
    }

    public boolean hasInvalidEntryWidgets() {
        return entryWidgets.values().stream().anyMatch(w -> !w.isValid());
    }

    public Map<String, Object> collectTouchedPathsForSync() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String path : syncTouchedPaths) {
            ConfigEditorEntryWidget w = entryWidgets.get(path);
            if (w == null || !w.isValid()) {
                continue;
            }
            Object v = w.getValue();
            if (v != null) {
                map.put(path, v);
            }
        }
        return map;
    }

    public Map<String, Object> collectAllEntryValuesForSync() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (ConfigEntryDescriptor d : holder.getDescriptors()) {
            String path = d.getPath();
            ConfigEditorEntryWidget w = entryWidgets.get(path);
            if (w != null) {
                if (!w.isValid()) {
                    continue;
                }
                Object v = w.getValue();
                if (v != null) {
                    map.put(path, v);
                }
            } else {
                Object v = holder.get(path);
                if (v != null) {
                    map.put(path, v);
                }
            }
        }
        return map;
    }

    public void refreshEntriesFromHolder(String configName) {
        if (!holder.getConfigName().equals(configName)) {
            return;
        }
        for (Map.Entry<String, ConfigEditorEntryWidget> e : entryWidgets.entrySet()) {
            Object v = holder.get(e.getKey());
            if (v == null) {
                ConfigEntryDescriptor d = holder.getDescriptor(e.getKey());
                if (d != null) {
                    v = d.getDefaultValue();
                }
            }
            if (v != null) {
                e.getValue().setValue(v);
            }
        }
        markClean();
    }

    private static Object snapshot(Object value) {
        if (value instanceof List) {
            return new ArrayList<>((List<?>) value);
        }
        if (value instanceof Set) {
            return new LinkedHashSet<>((Set<?>) value);
        }
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<?, ?>) value);
        }
        return value;
    }
}
