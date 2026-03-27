package xin.vanilla.banira.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public final class NotificationManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String LOG_FILE_NAME = "notification_log.json";

    private final EnumMap<EnumPosition, List<Notification>> notifications = new EnumMap<>(EnumPosition.class);
    private final List<NotificationLogEntry> log = new CopyOnWriteArrayList<>();
    private static final NotificationManager instance = new NotificationManager();

    /**
     * 获取通知管理器实例
     */
    public static NotificationManager get() {
        return instance;
    }

    /**
     * 添加通知
     */
    public void addNotification(Notification notification) {
        addNotification(notification, false);
    }

    /**
     * 添加通知
     *
     * @param notification 通知
     * @param fromNetwork  是否来自服务端推送
     */
    public void addNotification(Notification notification, boolean fromNetwork) {
        this.notifications.computeIfAbsent(notification.position(), k -> new ArrayList<>()).add(notification);
        appendLog(notification, fromNetwork);
    }

    /**
     * 获取持久化的通知日志
     */
    public List<NotificationLogEntry> getLog() {
        return Collections.unmodifiableList(new ArrayList<>(log));
    }

    /**
     * 追加日志并持久化
     */
    private void appendLog(Notification notification, boolean fromNetwork) {
        long id = System.currentTimeMillis();
        String componentJson = JsonUtils.toString(AbstractComponent.serialize(notification.component()));
        NotificationLogEntry entry = new NotificationLogEntry()
                .id(id)
                .timestamp(id)
                .componentJson(componentJson)
                .positionName(notification.position().name())
                .animationName(notification.animation().name())
                .durationTime(notification.durationTime())
                .styleName(notification.style() != null ? notification.style().name() : "NORMAL")
                .source(fromNetwork ? "network" : "local");
        synchronized (log) {
            log.add(0, entry);
            int max = notificationLogMaxEntries();
            while (log.size() > max) {
                log.remove(log.size() - 1);
            }
        }
        saveLogAsync();
    }

    /**
     * 从文件加载日志
     */
    public void loadLog() {
        Path path = CustomConfig.getConfigDirectory().resolve(LOG_FILE_NAME);
        File file = path.toFile();
        if (!file.exists()) return;
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = JsonUtils.parseObject(content);
            JsonElement entriesEl = root.get("entries");
            if (entriesEl == null || !entriesEl.isJsonArray()) return;
            JsonArray arr = entriesEl.getAsJsonArray();
            synchronized (log) {
                log.clear();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    NotificationLogEntry entry = new NotificationLogEntry()
                            .id(JsonUtils.getLong(obj, "id", 0))
                            .timestamp(JsonUtils.getLong(obj, "timestamp", 0))
                            .componentJson(JsonUtils.getString(obj, "componentJson", "{}"))
                            .positionName(JsonUtils.getString(obj, "positionName", "TOP_RIGHT"))
                            .animationName(JsonUtils.getString(obj, "animationName", "AUTO"))
                            .durationTime(JsonUtils.getLong(obj, "durationTime", 5000))
                            .styleName(JsonUtils.getString(obj, "styleName", "NORMAL"))
                            .source(JsonUtils.getString(obj, "source", "local"));
                    log.add(entry);
                }
                int max = notificationLogMaxEntries();
                while (log.size() > max) {
                    log.remove(log.size() - 1);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load notification log: {}", e.getMessage());
        }
    }

    /**
     * 异步保存日志到文件
     */
    private static int notificationLogMaxEntries() {
        return Math.max(1, ClientConfig.get().notificationLogMaxEntries());
    }

    private void saveLogAsync() {
        new Thread(() -> {
            try {
                Path dir = CustomConfig.getConfigDirectory();
                Files.createDirectories(dir);
                Path path = dir.resolve(LOG_FILE_NAME);
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                List<NotificationLogEntry> snapshot;
                synchronized (log) {
                    snapshot = new ArrayList<>(log);
                }
                for (NotificationLogEntry e : snapshot) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", e.id());
                    obj.addProperty("timestamp", e.timestamp());
                    obj.addProperty("componentJson", e.componentJson());
                    obj.addProperty("positionName", e.positionName());
                    obj.addProperty("animationName", e.animationName());
                    obj.addProperty("durationTime", e.durationTime());
                    obj.addProperty("styleName", e.styleName() != null ? e.styleName() : "NORMAL");
                    obj.addProperty("source", e.source());
                    arr.add(obj);
                }
                root.add("entries", arr);
                Files.write(path, JsonUtils.toPrettyString(root).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOGGER.warn("Failed to save notification log: {}", e.getMessage());
            }
        }).start();
    }

    @OnlyIn(Dist.CLIENT)
    public void render(MatrixStack stack) {
        Minecraft mc = Minecraft.getInstance();
        ScreenCoordinate screenInfo = new ScreenCoordinate()
                .width(mc.getWindow().getGuiScaledWidth())
                .height(mc.getWindow().getGuiScaledHeight());
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<EnumPosition, List<Notification>> entry : notifications.entrySet()) {
            entry.getValue().removeIf(Notification::finished);

            EnumPosition pos = entry.getKey();
            List<Notification> list = entry.getValue().stream().filter(n -> n.scheduledTime() <= currentTime).collect(Collectors.toList());

            // 初始化布局上下文
            boolean stacksDown = pos == EnumPosition.TOP_LEFT || pos == EnumPosition.TOP_CENTER || pos == EnumPosition.TOP_RIGHT
                    || pos == EnumPosition.LEFT_CENTER || pos == EnumPosition.RIGHT_CENTER || pos == EnumPosition.CENTER;
            ScreenCoordinate preInfo = new ScreenCoordinate()
                    .y(stacksDown ? 0 : screenInfo.height())
                    .height(0);

            int i = 0;
            Iterator<Notification> iter = list.iterator();
            while (iter.hasNext()) {
                Notification n = iter.next();

                // 状态过滤
                if (n.finished()) {
                    iter.remove();
                    continue;
                }

                // 第一项且为居中位置时，设置 preInfo 使首项垂直居中
                if (i == 0 && (pos == EnumPosition.CENTER || pos == EnumPosition.LEFT_CENTER || pos == EnumPosition.RIGHT_CENTER)) {
                    preInfo.y((screenInfo.height() - n.cachedHeight()) / 2 - n.margin());
                }

                // 位置预计算
                ScreenCoordinate lastInfo = n.calculatePosition(screenInfo, preInfo);

                // 是否可见
                if (this.shouldSkipRendering(pos, lastInfo, screenInfo)) {
                    break;
                }

                // 执行渲染
                n.index(i++).render(stack, preInfo, screenInfo, currentTime);

                // 更新布局上下文
                preInfo.y(n.lastY());
                preInfo.width(n.cachedWidth());
                preInfo.height(n.cachedHeight());
            }
        }
    }

    /**
     * 判断是否需要跳过渲染
     *
     * @param pos        位置
     * @param coordinate 布局信息
     * @param screenInfo 屏幕信息
     */
    private boolean shouldSkipRendering(EnumPosition pos, ScreenCoordinate coordinate, ScreenCoordinate screenInfo) {
        switch (pos) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
            case LEFT_CENTER:
            case RIGHT_CENTER:
            case CENTER:
                return coordinate.y() + coordinate.height() > screenInfo.height();
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                return coordinate.y() < 0;
            default:
                return false;
        }
    }
}
