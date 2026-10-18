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
    private final Map<String, String> baselineDrafts = new LinkedHashMap<>();
    private final Map<String, Object> retainedValues = new LinkedHashMap<>();
    private final Map<String, String> retainedDrafts = new LinkedHashMap<>();
    private Map<String, Object> remoteValues;

    public ConfigEditorState(ConfigHolder holder) {
        this.holder = holder;
    }

    public void clearEntries() {
        entryWidgets.forEach((path, widget) -> {
            retainedValues.put(path, snapshot(widget.getValue()));
            if (widget.getDraft() != null) retainedDrafts.put(path, widget.getDraft());
            else retainedDrafts.remove(path);
        });
        entryWidgets.clear();
    }

    public void clearPendingChanges() {
        modifiedValues.clear();
        syncTouchedPaths.clear();
    }

    public void registerEntry(String path, ConfigEditorEntryWidget widget) {
        if (path != null && widget != null) {
            if (retainedValues.containsKey(path)) {
                widget.setValue(snapshot(retainedValues.get(path)));
            } else if (remoteValues != null && remoteValues.containsKey(path)) {
                widget.setValue(snapshot(remoteValues.get(path)));
            }
            entryWidgets.put(path, widget);
            baselineValues.putIfAbsent(path, snapshot(widget.getValue()));
            if (!baselineDrafts.containsKey(path)) baselineDrafts.put(path, widget.getDraft());
            if (retainedDrafts.containsKey(path)) widget.setDraft(retainedDrafts.get(path));
        }
    }

    public void markModified(String path, Object value) {
        if (path != null) {
            ConfigEditorEntryWidget widget = entryWidgets.get(path);
            if ((widget == null || widget.isValid()) && Objects.deepEquals(baselineValues.get(path), value)) {
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
                if (!Objects.equals(entry.getValue().getDraft(), baselineDrafts.get(entry.getKey()))) paths.add(entry.getKey());
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
        retainedValues.clear();
        retainedDrafts.clear();
        for (Map.Entry<String, ConfigEditorEntryWidget> entry : entryWidgets.entrySet()) {
            baselineValues.put(entry.getKey(), snapshot(entry.getValue().getValue()));
            baselineDrafts.put(entry.getKey(), entry.getValue().getDraft());
            if (remoteValues != null) remoteValues.put(entry.getKey(), snapshot(entry.getValue().getValue()));
        }
    }

    public void applyModifiedToHolder() {
        if (isRemoteSnapshot()) throw new IllegalStateException("Remote configuration must be sent to its server");
        holder.setAll(modifiedValues, xin.vanilla.banira.common.config.ConfigEditOrigin.UI);
    }

    public boolean isRemoteSnapshot() {
        return remoteValues != null;
    }

    public void acceptRemoteSnapshot(String configName, Map<String, Object> values) {
        if (!holder.getConfigName().equals(configName)) return;
        Set<String> dirty = dirtyPaths();
        remoteValues = new LinkedHashMap<>();
        values.forEach((path, value) -> remoteValues.put(path, snapshot(value)));
        entryWidgets.forEach((path, widget) -> {
            if (remoteValues.containsKey(path)) {
                if (!dirty.contains(path)) widget.setValue(snapshot(remoteValues.get(path)));
                baselineValues.put(path, snapshot(remoteValues.get(path)));
                if (!dirty.contains(path)) baselineDrafts.put(path, widget.getDraft());
            }
        });
        retainedValues.clear();
        collectModifiedFromWidgets();
    }

    public boolean hasInvalidEntryWidgets() {
        return entryWidgets.values().stream().anyMatch(w -> !w.isValid());
    }

    public Map<String, Object> collectTouchedPathsForSync() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String path : dirtyPaths()) {
            ConfigEditorEntryWidget w = entryWidgets.get(path);
            if (w == null || !w.isValid()) {
                continue;
            }
            Object v = w.getValue();
            if (v != null && !Objects.deepEquals(v, baselineValues.get(path))) {
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
                Object v = remoteValues != null ? remoteValues.get(path) : holder.get(path);
                if (v != null) {
                    map.put(path, v);
                }
            }
        }
        return map;
    }

    public void refreshEntriesFromHolder(String configName) {
        if (isRemoteSnapshot() || !holder.getConfigName().equals(configName)) {
            return;
        }
        Set<String> dirty = dirtyPaths();
        for (Map.Entry<String, ConfigEditorEntryWidget> e : entryWidgets.entrySet()) {
            Object v = holder.get(e.getKey());
            if (v == null) {
                ConfigEntryDescriptor d = holder.getDescriptor(e.getKey());
                if (d != null) {
                    v = d.getDefaultValue();
                }
            }
            if (v != null) {
                if (!dirty.contains(e.getKey())) e.getValue().setValue(snapshot(v));
                baselineValues.put(e.getKey(), snapshot(v));
                if (!dirty.contains(e.getKey())) baselineDrafts.put(e.getKey(), e.getValue().getDraft());
            }
        }
        retainedValues.clear();
        collectModifiedFromWidgets();
    }

    private Set<String> dirtyPaths() {
        Set<String> paths = new LinkedHashSet<>(syncTouchedPaths);
        entryWidgets.forEach((path, widget) -> {
            if (!Objects.deepEquals(widget.getValue(), baselineValues.get(path))) paths.add(path);
            if (!widget.isValid() && !Objects.equals(widget.getDraft(), baselineDrafts.get(path))) paths.add(path);
        });
        return paths;
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
