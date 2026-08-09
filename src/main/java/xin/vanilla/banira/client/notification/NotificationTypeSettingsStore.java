package xin.vanilla.banira.client.notification;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.util.CoalescingAsyncTask;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端按通知类型保存的显示偏好（隐藏、时长、动画、位置）。
 */
public final class NotificationTypeSettingsStore {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FILE_NAME = "notification_type_settings.json";

    private static final NotificationTypeSettingsStore INSTANCE = new NotificationTypeSettingsStore();

    private final Map<String, TypeSettings> byType = new ConcurrentHashMap<>();
    private final CoalescingAsyncTask saveTask = new CoalescingAsyncTask(
            "BaniraCodex-NotificationTypeSettingsSave",
            this::saveSnapshot,
            e -> LOGGER.warn("Failed to save notification type settings: {}", e.getMessage())
    );

    /**
     * 是否已完成至少一次磁盘加载尝试
     */
    @Getter
    private volatile boolean settingsLoadedFromDisk;

    public static NotificationTypeSettingsStore get() {
        return INSTANCE;
    }

    public void load() {
        Path path = CustomConfig.getConfigDirectory().resolve(FILE_NAME);
        File file = path.toFile();
        if (!file.exists()) {
            markSettingsLoadedAndApplyRegistryDefaults();
            return;
        }
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = JsonUtils.parseObject(content);
            JsonElement typesEl = root.get("types");
            if (typesEl == null || !typesEl.isJsonObject()) {
                markSettingsLoadedAndApplyRegistryDefaults();
                return;
            }
            JsonObject types = typesEl.getAsJsonObject();
            synchronized (byType) {
                byType.clear();
                for (Map.Entry<String, JsonElement> e : types.entrySet()) {
                    if (!e.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject o = e.getValue().getAsJsonObject();
                    TypeSettings s = new TypeSettings()
                            .hidden(JsonUtils.getBoolean(o, "hidden", false))
                            .durationMs(JsonUtils.getLong(o, "durationMs", 0))
                            .positionName(JsonUtils.getString(o, "positionName", ""))
                            .animationName(JsonUtils.getString(o, "animationName", ""))
                            .displayMode(EnumNotificationTypeDisplayMode.parseOrDefault(JsonUtils.getString(o, "displayMode", "")));
                    byType.put(NotificationTypeKeys.normalizeOrDefault(e.getKey()), s);
                }
            }
            markSettingsLoadedAndApplyRegistryDefaults();
        } catch (Exception e) {
            LOGGER.warn("Failed to load notification type settings: {}", e.getMessage());
            markSettingsLoadedAndApplyRegistryDefaults();
        }
    }

    private void markSettingsLoadedAndApplyRegistryDefaults() {
        settingsLoadedFromDisk = true;
        NotificationTypeRegistry.applyAllResolvedDefaultsAfterStoreLoad();
    }

    /**
     * 若已加载的 JSON 中不存在该类型条目，则按 {@link NotificationTypeRegistry#resolvedDisplayDefault(String)} 写入 {@code displayMode} 并保存。
     */
    public void applyResolvedDisplayDefaultIfNoSavedEntry(String typeId) {
        String t = NotificationTypeKeys.normalizeOrDefault(typeId);
        EnumNotificationTypeDisplayMode mode = NotificationTypeRegistry.resolvedDisplayDefault(t);
        if (mode == null) {
            return;
        }
        synchronized (byType) {
            if (byType.containsKey(t)) {
                return;
            }
            byType.put(t, new TypeSettings().displayMode(mode));
        }
        saveAsync();
    }

    public void saveAsync() {
        saveTask.request();
    }

    private void saveSnapshot() throws Exception {
        Path dir = CustomConfig.getConfigDirectory();
        Files.createDirectories(dir);
        Path path = dir.resolve(FILE_NAME);
        JsonObject root = new JsonObject();
        JsonObject types = new JsonObject();
        Map<String, TypeSettings> snapshot;
        synchronized (byType) {
            snapshot = new LinkedHashMap<>(byType);
        }
        for (Map.Entry<String, TypeSettings> e : snapshot.entrySet()) {
            TypeSettings s = e.getValue();
            JsonObject o = new JsonObject();
            o.addProperty("hidden", s.hidden());
            o.addProperty("durationMs", s.durationMs());
            o.addProperty("positionName", s.positionName() != null ? s.positionName() : "");
            o.addProperty("animationName", s.animationName() != null ? s.animationName() : "");
            o.addProperty("displayMode", s.displayMode() != null ? s.displayMode().name() : EnumNotificationTypeDisplayMode.OVERLAY.name());
            types.add(e.getKey(), o);
        }
        root.add("types", types);
        Files.write(path, JsonUtils.toPrettyString(root).getBytes(StandardCharsets.UTF_8));
        ManagedConfigFiles.markWritten(path);
    }

    public TypeSettings getOrCreate(String typeId) {
        String t = NotificationTypeKeys.normalizeOrDefault(typeId);
        return byType.computeIfAbsent(t, k -> new TypeSettings());
    }

    public void put(String typeId, TypeSettings settings) {
        byType.put(NotificationTypeKeys.normalizeOrDefault(typeId), copyOf(settings));
        saveAsync();
    }

    /**
     * 返回与存储解耦的快照，供配置界面在保存前编辑草稿。
     */
    public Map<String, TypeSettings> snapshot() {
        Map<String, TypeSettings> snapshot = new LinkedHashMap<>();
        synchronized (byType) {
            byType.forEach((typeId, settings) -> snapshot.put(typeId, copyOf(settings)));
        }
        return snapshot;
    }

    /**
     * 用一次完整提交替换当前配置，避免每改一个控件就异步写盘。
     */
    public void replaceAllAndSave(Map<String, TypeSettings> settingsByType) {
        synchronized (byType) {
            byType.clear();
            if (settingsByType != null) {
                settingsByType.forEach((typeId, settings) ->
                        byType.put(NotificationTypeKeys.normalizeOrDefault(typeId), copyOf(settings)));
            }
        }
        saveAsync();
    }

    public static TypeSettings copyOf(TypeSettings settings) {
        TypeSettings source = settings != null ? settings : new TypeSettings();
        return new TypeSettings()
                .hidden(source.hidden())
                .durationMs(source.durationMs())
                .positionName(source.positionName() != null ? source.positionName() : "")
                .animationName(source.animationName() != null ? source.animationName() : "")
                .displayMode(source.displayMode() != null
                        ? source.displayMode()
                        : EnumNotificationTypeDisplayMode.OVERLAY);
    }

    public Set<String> typeIdsFromStored() {
        synchronized (byType) {
            return Collections.unmodifiableSet(byType.keySet());
        }
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class TypeSettings {
        private boolean hidden;
        /**
         * 0 表示使用本次通知携带的时长
         */
        private long durationMs;
        /**
         * 空串表示使用本次通知携带的位置
         */
        private String positionName = "";
        /**
         * 空串表示使用本次通知携带的动画
         */
        private String animationName = "";
        /**
         * 收到网络通知时的客户端展示方式
         */
        private EnumNotificationTypeDisplayMode displayMode = EnumNotificationTypeDisplayMode.OVERLAY;
    }
}
