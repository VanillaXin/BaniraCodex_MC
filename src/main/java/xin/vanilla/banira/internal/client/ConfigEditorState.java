package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;

import java.util.*;

public final class ConfigEditorState {
    private final ConfigHolder holder;
    private final Map<String, Object> modifiedValues = new LinkedHashMap<>();
    private final Map<String, ConfigEditorEntryWidget> entryWidgets = new LinkedHashMap<>();
    private final Set<String> syncTouchedPaths = new LinkedHashSet<>();

    public ConfigEditorState(ConfigHolder holder) {
        this.holder = holder;
    }

    public void clearEntries() {
        entryWidgets.clear();
        syncTouchedPaths.clear();
    }

    public void clearPendingChanges() {
        modifiedValues.clear();
        syncTouchedPaths.clear();
    }

    public void clearModifiedValues() {
        modifiedValues.clear();
    }

    public void registerEntry(String path, ConfigEditorEntryWidget widget) {
        if (path != null && widget != null) {
            entryWidgets.put(path, widget);
        }
    }

    public void markModified(String path, Object value) {
        if (path != null) {
            modifiedValues.put(path, value);
            syncTouchedPaths.add(path);
        }
    }

    public void collectModifiedFromWidgets() {
        for (Map.Entry<String, ConfigEditorEntryWidget> e : entryWidgets.entrySet()) {
            if (!e.getValue().isValid()) {
                continue;
            }
            Object v = e.getValue().getValue();
            if (v != null && !Objects.equals(v, holder.get(e.getKey()))) {
                modifiedValues.put(e.getKey(), v);
            }
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
        clearPendingChanges();
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
    }
}
