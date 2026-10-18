package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.common.util.JsonUtils;
import java.util.ArrayList;
import java.util.List;

/** Retained interaction state; committed list values stay in the configuration editor. */
public final class ConfigListDraft {
    public boolean expanded, adding;
    public double scroll;
    public int editingIndex = -1;
    public String editText, raw, undo;
    public String addText = "";
    public List<String> selected = new ArrayList<>();

    public boolean pending() {
        return raw != null || editingIndex >= 0 || adding && (!addText.isEmpty() || !selected.isEmpty());
    }

    public String encode() { return JsonUtils.GSON.toJson(this); }

    public static ConfigListDraft decode(String encoded) {
        try {
            ConfigListDraft draft = JsonUtils.GSON.fromJson(encoded, ConfigListDraft.class);
            if (draft == null) throw new IllegalArgumentException("Missing list draft");
            if (draft.addText == null) draft.addText = "";
            if (draft.selected == null) draft.selected = new ArrayList<>();
            if (!Double.isFinite(draft.scroll)) draft.scroll = 0;
            return draft;
        } catch (RuntimeException invalid) {
            ConfigListDraft draft = new ConfigListDraft(); draft.raw = encoded; return draft;
        }
    }
}
