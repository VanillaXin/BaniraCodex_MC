package xin.vanilla.banira.internal.client;

import org.junit.Test;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class ConfigEditorStateTest {

    @Test public void remoteSnapshotStaysInEditorAcrossRebuildAndCannotBeSavedLocally() {
        ConfigHolder holder = holder();
        ConfigEditorState state = new ConfigEditorState(holder);
        TestWidget widget = new TestWidget("a");
        state.registerEntry("first", widget);
        state.acceptRemoteSnapshot("wrong", Collections.singletonMap("first", "ignored"));
        assertEquals("a", widget.value);
        state.acceptRemoteSnapshot(holder.getConfigName(), Collections.singletonMap("first", "script:serverOnly"));
        assertEquals("script:serverOnly", widget.value);
        assertEquals("a", holder.get("first"));
        assertEquals(0, state.pendingChangeCount());
        state.refreshEntriesFromHolder(holder.getConfigName());
        assertEquals("script:serverOnly", widget.value);
        widget.value = "script:changed";
        state.markModified("first", widget.value);
        assertEquals("script:changed", state.collectTouchedPathsForSync().get("first"));
        state.markClean();
        state.clearEntries();
        TestWidget rebuilt = new TestWidget("a");
        state.registerEntry("first", rebuilt);
        assertEquals("script:changed", rebuilt.value);
        state.collectModifiedFromWidgets();
        try {
            state.applyModifiedToHolder();
            org.junit.Assert.fail("Remote editor must never save into local configuration");
        } catch (IllegalStateException expected) { }
        assertEquals("a", holder.get("first"));
        ConfigEditorState reopened = new ConfigEditorState(holder);
        TestWidget local = new TestWidget(holder.get("first"));
        reopened.registerEntry("first", local);
        assertEquals("a", local.value);
    }

    @Test
    public void countsOnlyValuesDifferentFromOpeningBaseline() {
        ConfigEditorState state = new ConfigEditorState(holder());
        TestWidget first = new TestWidget("a");
        TestWidget second = new TestWidget(1);
        state.registerEntry("first", first);
        state.registerEntry("second", second);

        assertEquals(0, state.pendingChangeCount());
        first.value = "b";
        state.markModified("first", "b");
        second.value = 2;
        state.markModified("second", 2);
        assertEquals(2, state.pendingChangeCount());

        first.value = "a";
        state.markModified("first", "a");
        assertEquals(1, state.pendingChangeCount());
    }

    @Test
    public void markCleanMovesBaselineAfterSaveOrSync() {
        ConfigEditorState state = new ConfigEditorState(holder());
        TestWidget widget = new TestWidget("a");
        state.registerEntry("first", widget);
        widget.value = "b";
        state.markModified("first", "b");

        state.markClean();

        assertEquals(0, state.pendingChangeCount());
        widget.value = "a";
        state.markModified("first", "a");
        assertEquals(1, state.pendingChangeCount());
    }

    @Test
    public void untouchedInvalidEntryDoesNotBlockEscape() {
        ConfigEditorState state = new ConfigEditorState(holder());
        TestWidget widget = new TestWidget("a");
        widget.valid = false;
        state.registerEntry("first", widget);

        assertEquals(0, state.pendingChangeCount());
    }

    private static ConfigHolder holder() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", "a");
        values.put("second", 1);
        return ConfigHolder.create("test", "test-common.toml", ConfigScope.COMMON,
                new MapStore(values), Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }

    private static final class TestWidget implements ConfigEditorEntryWidget {
        private Object value;
        private boolean valid = true;

        private TestWidget(Object value) {
            this.value = value;
        }

        @Override
        public BaseWidget getWidget() {
            return null;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return valid;
        }
    }

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values;

        private MapStore(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public Set<String> paths() {
            return values.keySet();
        }

        @Override
        public Object get(String path) {
            return values.get(path);
        }

        @Override
        public void set(String path, Object value) {
            values.put(path, value);
        }

        @Override public void setAll(Map<String, Object> changes) { values.putAll(changes); }

        @Override
        public Class<?> valueClass(String path) {
            Object value = values.get(path);
            return value != null ? value.getClass() : Object.class;
        }

        @Override
        public Object defaultValue(String path) {
            return null;
        }

        @Override
        public boolean validate(String path, Object value) {
            return true;
        }

        @Override
        public void save() {
        }
    }
}
