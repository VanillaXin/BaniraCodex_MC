package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;
import xin.vanilla.banira.internal.client.ConfigEditorEntryWidget;
import xin.vanilla.banira.internal.client.ConfigEditorState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiConfigParityTest {
    @Test
    public void reloadUpdatesUntouchedValuesButKeepsUnsavedEdits() {
        Store store = new Store();
        ConfigHolder holder = holder(store);
        ConfigEditorState state = new ConfigEditorState(holder);
        Entry edited = new Entry("a"), untouched = new Entry(1);
        state.registerEntry("first", edited);
        state.registerEntry("second", untouched);
        edited.value = "draft";
        state.markModified("first", edited.value);
        store.values.put("first", "external");
        store.values.put("second", 9);
        state.refreshEntriesFromHolder(holder.getConfigName());
        assertEquals("draft", edited.value);
        assertEquals(9, untouched.value);
        assertEquals(1, state.pendingChangeCount());
        assertEquals(Collections.singletonMap("first", "draft"), state.collectTouchedPathsForSync());
    }

    @Test
    public void rendererRebuildPreservesValuesAndOriginalBaseline() {
        ConfigEditorState state = new ConfigEditorState(holder(new Store()));
        Entry entry = new Entry("a");
        state.registerEntry("first", entry);
        entry.value = "draft";
        state.markModified("first", entry.value);
        state.clearEntries();
        Entry next = new Entry("a");
        state.registerEntry("first", next);
        assertEquals("draft", next.value);
        assertEquals(1, state.pendingChangeCount());
        next.value = "a";
        state.markModified("first", next.value);
        assertEquals(0, state.pendingChangeCount());
    }

    @Test
    public void remoteFetchDoesNotOverwriteTouchedInvalidInput() {
        ConfigEditorState state = new ConfigEditorState(holder(new Store()));
        Entry entry = new Entry("a");
        state.registerEntry("first", entry);
        entry.value = "partial";
        entry.valid = false;
        state.markModified("first", entry.value);
        state.acceptRemoteSnapshot("test-common.toml", Collections.singletonMap("first", "remote"));
        assertEquals("partial", entry.value);
        assertEquals(1, state.pendingChangeCount());
        assertTrue(state.hasInvalidEntryWidgets());
        assertTrue(state.collectTouchedPathsForSync().isEmpty());
    }

    @Test
    public void rendererRebuildKeepsInvalidRawDraftSeparatelyFromTypedValue() {
        ConfigEditorState state = new ConfigEditorState(holder(new Store()));
        Entry edited = new Entry(1);
        state.registerEntry("second", edited);
        edited.draft = "-";
        edited.valid = false;
        state.markModified("second", 1);
        state.clearEntries();
        Entry next = new Entry(1);
        state.registerEntry("second", next);
        assertEquals("-", next.draft);
        assertEquals(1, next.value);
        assertEquals(1, state.pendingChangeCount());
    }

    static ConfigHolder holder(Store store) {
        return ConfigHolder.create("test", "test-common.toml", ConfigScope.COMMON, store,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }

    static final class Entry implements ConfigEditorEntryWidget {
        Object value;
        boolean valid = true;
        String draft;

        Entry(Object value) {
            this.value = value;
        }

        public BaseWidget getWidget() {
            return null;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public boolean isValid() {
            return valid;
        }

        public String getDraft() {
            return draft;
        }

        public void setDraft(String draft) {
            this.draft = draft;
            valid = !"-".equals(draft);
        }
    }

    static final class Store implements ConfigValueStore {
        final Map<String, Object> values = new LinkedHashMap<>();
        boolean failSave;

        Store() {
            values.put("first", "a");
            values.put("second", 1);
        }

        public Set<String> paths() {
            return values.keySet();
        }

        public Object get(String path) {
            return values.get(path);
        }

        public void set(String path, Object value) {
            values.put(path, value);
        }

        public void setAll(Map<String, Object> changes) {
            values.putAll(changes);
        }

        public Class<?> valueClass(String path) {
            return values.get(path).getClass();
        }

        public Object defaultValue(String path) {
            return null;
        }

        public boolean validate(String path, Object value) {
            return true;
        }

        public void save() {
            if (failSave) throw new IllegalStateException("write rejected");
        }
    }
}
