package xin.vanilla.banira.internal.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists notification history without coupling rendering code to file IO.
 */
public final class NotificationLogStore {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String LOG_FILE_NAME = "notification_log.json";

    private static class WriterHolder {
        private static final NotificationLogWriter WRITER = new NotificationLogWriter(
                CustomConfig.getConfigDirectory().resolve(LOG_FILE_NAME));

        static {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    WRITER.close();
                } catch (IOException error) {
                    LOGGER.warn("Failed to flush notification log on shutdown", error);
                }
            }, "BaniraCodex-NotificationLogFlush"));
        }
    }

    private NotificationLogStore() {
    }

    public static List<NotificationLogEntry> load(int maxEntries) {
        Path path = CustomConfig.getConfigDirectory().resolve(LOG_FILE_NAME);
        return load(path, maxEntries);
    }

    static List<NotificationLogEntry> load(Path path, int maxEntries) {
        File file = path.toFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = JsonUtils.parseObject(content);
            JsonElement entriesEl = root.get("entries");
            if (entriesEl == null || !entriesEl.isJsonArray()) {
                return new ArrayList<>();
            }
            List<NotificationLogEntry> entries = new ArrayList<>();
            JsonArray arr = entriesEl.getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                entries.add(new NotificationLogEntry()
                        .id(JsonUtils.getLong(obj, "id", 0))
                        .timestamp(JsonUtils.getLong(obj, "timestamp", 0))
                        .componentJson(JsonUtils.getString(obj, "componentJson", "{}"))
                        .positionName(JsonUtils.getString(obj, "positionName", "TOP_RIGHT"))
                        .animationName(JsonUtils.getString(obj, "animationName", "AUTO"))
                        .durationTime(JsonUtils.getLong(obj, "durationTime", 5000))
                        .styleName(JsonUtils.getString(obj, "styleName", "NORMAL"))
                        .notificationType(JsonUtils.getString(obj, "notificationType", NotificationTypeKeys.DEFAULT))
                        .source(JsonUtils.getString(obj, "source", "local"))
                        .read(JsonUtils.getBoolean(obj, "read", true)));
            }
            trimToMax(entries, maxEntries);
            return entries;
        } catch (Exception e) {
            LOGGER.warn("Failed to load notification log: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void saveAsync(List<NotificationLogEntry> snapshot) {
        WriterHolder.WRITER.submit(snapshot);
    }

    public static void flush() throws IOException {
        WriterHolder.WRITER.flush();
    }

    static void save(Path path, List<NotificationLogEntry> snapshot) throws IOException {
        Path dir = path.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (NotificationLogEntry e : snapshot) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", e.id());
            obj.addProperty("timestamp", e.timestamp());
            obj.addProperty("componentJson", e.componentJson());
            obj.addProperty("positionName", e.positionName());
            obj.addProperty("animationName", e.animationName());
            obj.addProperty("durationTime", e.durationTime());
            obj.addProperty("styleName", e.styleName() != null ? e.styleName() : "NORMAL");
            obj.addProperty("notificationType", e.notificationType() != null ? e.notificationType() : NotificationTypeKeys.DEFAULT);
            obj.addProperty("source", e.source());
            obj.addProperty("read", e.read());
            arr.add(obj);
        }
        root.add("entries", arr);
        root.addProperty("version", 2);
        Path temporary = Files.createTempFile(dir, "notification-log-", ".tmp");
        try {
            Files.write(temporary, JsonUtils.toPrettyString(root).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void trimToMax(List<?> entries, int maxEntries) {
        int max = Math.max(1, maxEntries);
        while (entries.size() > max) {
            entries.remove(entries.size() - 1);
        }
    }
}
