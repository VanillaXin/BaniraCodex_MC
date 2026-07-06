package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BaniraConfigViewsTest {

    @Test
    public void categoryViewReadsWritesAndFallsBackToDefaults() {
        FakeHandle handle = new FakeHandle();
        handle.values.put("language.defaultLanguage", "zh_cn");

        LanguageDefaults defaults = new LanguageDefaults();
        LanguageView view = BaniraConfigViews.category(LanguageView.class, handle, "language", defaults,
                (leaf, value, bean) -> value != null ? value : ((LanguageDefaults) bean).defaultLanguage);

        assertEquals("zh_cn", view.defaultLanguage());
        assertSame(view, view.defaultLanguage("en_us"));
        assertEquals("en_us", handle.values.get("language.defaultLanguage"));

        handle.values.remove("language.defaultLanguage");
        assertEquals("en_us", view.defaultLanguage());
    }

    public interface LanguageView {
        String defaultLanguage();

        LanguageView defaultLanguage(String value);
    }

    private static final class LanguageDefaults {
        private final String defaultLanguage = "en_us";
    }

    private static final class FakeHandle implements BaniraConfigHandle {
        private final Map<String, Object> values = new LinkedHashMap<>();

        @Override
        public String getModId() {
            return "test";
        }

        @Override
        public String getConfigName() {
            return "test-common";
        }

        @Override
        public void save() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String path) {
            return (T) values.get(path);
        }

        @Override
        public void set(String path, Object value) {
            values.put(path, value);
        }

        @Override
        public Set<String> valuePaths() {
            return values.keySet();
        }

        @Override
        public boolean hasValue(String path) {
            return values.containsKey(path);
        }

        @Nullable
        @Override
        public String findValuePath(String key) {
            return values.containsKey(key) ? key : null;
        }

        @Override
        public Class<?> valueClass(String path) {
            Object value = values.get(path);
            return value == null ? Object.class : value.getClass();
        }

        @Nullable
        @Override
        public Object defaultValue(String path) {
            return null;
        }

        @Override
        public boolean validate(String path, Object value) {
            return true;
        }

        @Override
        public boolean setIfValid(String path, Object value) {
            set(path, value);
            return true;
        }
    }
}
