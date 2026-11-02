package xin.vanilla.banira.internal.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PlayerPreferenceState {
    private Object connection;
    private String uuid;
    private Map<String, String> confirmed;

    public synchronized boolean bind(Object nextConnection, String nextUuid) {
        if (nextConnection == null || !validUuid(nextUuid)) {
            boolean changed = connection != null;
            clear();
            return changed;
        }
        if (connection == nextConnection && Objects.equals(uuid, nextUuid)) return false;
        connection = nextConnection;
        uuid = nextUuid;
        confirmed = null;
        return true;
    }

    public synchronized boolean apply(Object origin, String currentUuid, Map<String, String> snapshot) {
        if (origin == null || origin != connection || !Objects.equals(uuid, currentUuid)
                || snapshot == null || snapshot.size() != 3 || !Objects.equals(uuid, snapshot.get("uuid"))) return false;
        String language = snapshot.get("language"), mode = snapshot.get("notificationReceiveMode");
        if (language == null || !language.matches("[A-Za-z0-9_-]{1,256}")
                || !("notification".equals(mode) || "vanilla_message".equals(mode))) return false;
        Map<String, String> values = new LinkedHashMap<>();
        values.put("language", language);
        values.put("notificationReceiveMode", mode);
        confirmed = Collections.unmodifiableMap(values);
        return true;
    }

    public synchronized Map<String, String> snapshot(String playerUuid) {
        return Objects.equals(uuid, playerUuid) ? confirmed : null;
    }

    public synchronized void clear() {
        connection = null;
        uuid = null;
        confirmed = null;
    }

    private static boolean validUuid(String value) {
        if (value == null) return false;
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
