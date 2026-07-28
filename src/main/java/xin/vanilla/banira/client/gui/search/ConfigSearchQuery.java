package xin.vanilla.banira.client.gui.search;

import java.util.Locale;

/**
 * 配置界面搜索条件，统一处理空白、大小写与匹配位置。
 */
public final class ConfigSearchQuery {

    private final String value;

    private ConfigSearchQuery(String value) {
        this.value = normalizeQuery(value);
    }

    public static ConfigSearchQuery of(String value) {
        return new ConfigSearchQuery(value);
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    public boolean matches(String... candidates) {
        if (isEmpty()) {
            return true;
        }
        if (candidates == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (indexIn(candidate) >= 0) {
                return true;
            }
        }
        return false;
    }

    public int indexIn(String candidate) {
        return indexIn(candidate, 0);
    }

    public int indexIn(String candidate, int fromIndex) {
        if (isEmpty() || candidate == null) {
            return -1;
        }
        return candidate.toLowerCase(Locale.ROOT).indexOf(value, Math.max(0, fromIndex));
    }

    public int length() {
        return value.length();
    }

    private static String normalizeQuery(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
