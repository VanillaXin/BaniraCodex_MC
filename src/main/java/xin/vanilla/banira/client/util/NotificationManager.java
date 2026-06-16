package xin.vanilla.banira.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Style;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.notification.NotificationClientDisplay;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CustomConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Accessors(fluent = true)
public final class NotificationManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String LOG_FILE_NAME = "notification_log.json";

    private final EnumMap<EnumPosition, List<Notification>> notifications = new EnumMap<>(EnumPosition.class);
    private final List<NotificationLogEntry> log = new CopyOnWriteArrayList<>();
    private static final NotificationManager instance = new NotificationManager();

    private final List<Notification> frameDrawOrder = new ArrayList<>();
    private Style frameHoverStyle;
    private final CoalescingAsyncTask logSaveTask = new CoalescingAsyncTask(
            "BaniraCodex-NotificationLogSave",
            this::saveLogSnapshot,
            e -> LOGGER.warn("Failed to save notification log: {}", e.getMessage())
    );

    private static boolean prevLeftDown;

    public static NotificationManager get() {
        return instance;
    }

    public void addNotification(Notification notification) {
        addNotification(notification, false);
    }

    public void addNotification(Notification notification, boolean fromNetwork) {
        NotificationTypeRegistry.ensureKnown(notification.notificationType());
        applyTypeSettings(notification);
        long logId = System.currentTimeMillis() ^ (long) System.nanoTime();
        if (logId == 0) {
            logId = System.currentTimeMillis();
        }
        notification.logEntryId(logId);
        if (!isTypeHidden(notification.notificationType())) {
            if (NotificationClientDisplay.deliverVanillaIfConfigured(notification.component(), notification.notificationType())) {
                // 已由原版聊天/操作栏展示，不加入浮层
            } else {
                long nowMs = System.currentTimeMillis();
                ensureCoalesceFields(notification);
                if (tryMergeOverlayDuplicate(notification, nowMs)) {
                    appendLog(notification, fromNetwork);
                    return;
                }
                applyBurstStagger(notification, nowMs);
                notification.coalesceLastActivityMs(nowMs);
                this.notifications.computeIfAbsent(notification.position(), k -> new ArrayList<>()).add(notification);
            }
        }
        appendLog(notification, fromNetwork);
    }

    private static void ensureCoalesceFields(Notification n) {
        if (n.mergeBaseComponent() == null) {
            n.mergeBaseComponent(n.component() != null ? n.component().clone() : null);
        }
        if (n.coalesceKey() == null) {
            n.coalesceKey(buildCoalesceKey(n));
        }
    }

    private static String buildCoalesceKey(Notification n) {
        String type = n.notificationType() != null ? n.notificationType() : NotificationTypeKeys.DEFAULT;
        Component base = n.mergeBaseComponent() != null ? n.mergeBaseComponent() : n.component();
        if (base == null) {
            return type + '\0' + "{}";
        }
        return type + '\0' + JsonUtils.toString(AbstractComponent.serialize(base));
    }

    private Notification findCoalesceTarget(Notification incoming, long nowMs, int windowMs) {
        String key = incoming.coalesceKey();
        if (key == null) {
            return null;
        }
        for (List<Notification> list : notifications.values()) {
            for (Notification n : list) {
                if (n.finished()) {
                    continue;
                }
                if (!key.equals(n.coalesceKey())) {
                    continue;
                }
                if (nowMs - n.coalesceLastActivityMs() <= windowMs) {
                    return n;
                }
            }
        }
        return null;
    }

    private boolean tryMergeOverlayDuplicate(Notification incoming, long nowMs) {
        int windowMs = ClientConfig.get().notificationMergeWindowMs();
        if (windowMs <= 0) {
            return false;
        }
        Notification target = findCoalesceTarget(incoming, nowMs, windowMs);
        if (target == null) {
            return false;
        }
        target.absorbDuplicateFrom(incoming);
        target.coalesceLastActivityMs(nowMs);
        return true;
    }

    private void applyBurstStagger(Notification n, long nowMs) {
        ClientConfig.RootView cfg = ClientConfig.get();
        int stagger = cfg.notificationBurstStaggerMs();
        if (stagger <= 0) {
            return;
        }
        int th = Math.max(1, cfg.notificationBurstThreshold());
        int pending = countActiveNotifications();
        if (pending < th) {
            return;
        }
        long extra = (long) (pending - th + 1) * stagger;
        int cap = cfg.notificationBurstMaxExtraDelayMs();
        if (cap > 0) {
            extra = Math.min(extra, cap);
        }
        n.scheduledTime(Math.max(n.scheduledTime(), nowMs + extra));
    }

    private int countActiveNotifications() {
        int c = 0;
        for (List<Notification> list : notifications.values()) {
            for (Notification n : list) {
                if (!n.finished()) {
                    c++;
                }
            }
        }
        return c;
    }

    private static void applyTypeSettings(Notification n) {
        NotificationTypeSettingsStore.TypeSettings s = NotificationTypeSettingsStore.get().getOrCreate(n.notificationType());
        if (s.durationMs() > 0) {
            n.durationTime(s.durationMs());
        }
        if (s.positionName() != null && !s.positionName().isEmpty()) {
            EnumPosition p = EnumPosition.valueOfEx(s.positionName());
            if (p != null) {
                n.position(p);
            }
        }
        if (s.animationName() != null && !s.animationName().isEmpty()) {
            try {
                n.animation(EnumMoveType.valueOf(s.animationName()));
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isTypeHidden(String typeId) {
        return NotificationTypeSettingsStore.get().getOrCreate(typeId).hidden();
    }

    public List<NotificationLogEntry> getLog() {
        return List.copyOf(log);
    }

    private void appendLog(Notification notification, boolean fromNetwork) {
        String componentJson = JsonUtils.toString(AbstractComponent.serialize(notification.component()));
        NotificationLogEntry entry = new NotificationLogEntry()
                .id(notification.logEntryId())
                .timestamp(System.currentTimeMillis())
                .componentJson(componentJson)
                .positionName(notification.position().name())
                .animationName(notification.animation().name())
                .durationTime(notification.durationTime())
                .styleName(notification.style() != null ? notification.style().name() : "NORMAL")
                .notificationType(notification.notificationType() != null ? notification.notificationType() : NotificationTypeKeys.DEFAULT)
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

    public void loadLog() {
        Path path = CustomConfig.getConfigDirectory().resolve(LOG_FILE_NAME);
        File file = path.toFile();
        if (!file.exists()) return;
        try {
            String content = Files.readString(path);
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
                            .notificationType(JsonUtils.getString(obj, "notificationType", NotificationTypeKeys.DEFAULT))
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

    private static int notificationLogMaxEntries() {
        return Math.max(1, ClientConfig.get().notificationLogMaxEntries());
    }

    private void saveLogAsync() {
        logSaveTask.request();
    }

    private void saveLogSnapshot() throws Exception {
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
            obj.addProperty("notificationType", e.notificationType() != null ? e.notificationType() : NotificationTypeKeys.DEFAULT);
            obj.addProperty("source", e.source());
            arr.add(obj);
        }
        root.add("entries", arr);
        Files.writeString(path, JsonUtils.toPrettyString(root));
    }

    @Environment(EnvType.CLIENT)
    public void render(PoseStack stack) {
        xin.vanilla.banira.common.data.KeyValue<Integer, Integer> screenSize = AbstractGuiUtils.getGuiScaledSize();
        ScreenCoordinate screenInfo = new ScreenCoordinate()
                .width(screenSize.key())
                .height(screenSize.val());
        long currentTime = System.currentTimeMillis();

        frameDrawOrder.clear();
        frameHoverStyle = null;

        double mx = BaniraClientRuntime.scaledMouseX();
        double my = BaniraClientRuntime.scaledMouseY();

        for (Map.Entry<EnumPosition, List<Notification>> entry : notifications.entrySet()) {
            EnumPosition pos = entry.getKey();
            List<Notification> list = entry.getValue();

            boolean stacksDown = pos == EnumPosition.TOP_LEFT || pos == EnumPosition.TOP_CENTER || pos == EnumPosition.TOP_RIGHT
                    || pos == EnumPosition.LEFT_CENTER || pos == EnumPosition.RIGHT_CENTER || pos == EnumPosition.CENTER;
            ScreenCoordinate preInfo = new ScreenCoordinate()
                    .y(stacksDown ? 0 : screenInfo.height())
                    .height(0);

            int i = 0;
            Iterator<Notification> iter = list.iterator();
            while (iter.hasNext()) {
                Notification n = iter.next();

                if (n.finished()) {
                    iter.remove();
                    continue;
                }
                if (n.scheduledTime() > currentTime) {
                    continue;
                }

                if (i == 0 && (pos == EnumPosition.CENTER || pos == EnumPosition.LEFT_CENTER || pos == EnumPosition.RIGHT_CENTER)) {
                    preInfo.y((screenInfo.height() - n.cachedHeight()) / 2 - n.margin());
                }

                ScreenCoordinate lastInfo = n.calculatePosition(screenInfo, preInfo);

                if (this.shouldSkipRendering(pos, lastInfo, screenInfo)) {
                    break;
                }

                frameDrawOrder.add(n);
                n.index(i++).renderAt(stack, preInfo, screenInfo, currentTime, lastInfo);

                preInfo.y(n.lastY());
                preInfo.width(n.cachedWidth());
                preInfo.height(n.cachedHeight());
            }
        }

        for (int idx = frameDrawOrder.size() - 1; idx >= 0; idx--) {
            Notification n = frameDrawOrder.get(idx);
            if (n.finished()) {
                continue;
            }
            if (!n.containsPoint(mx, my)) {
                continue;
            }
            Style st = n.styleAtTextPoint(mx, my);
            if (st != null && st.getHoverEvent() != null) {
                frameHoverStyle = st;
                break;
            }
        }

        if (frameHoverStyle != null) {
            NotificationStyleInteractionHelper.renderHoverTooltip(stack, (int) mx, (int) my,
                    (int) screenInfo.width(), (int) screenInfo.height(), frameHoverStyle);
        }
    }

    /**
     * 事件层只持有 opaque nativeGraphics；当前 Forge 1.18.2 分支内部仍使用 PoseStack 渲染通知。
     */
    @Environment(EnvType.CLIENT)
    public void renderNative(Object nativeGraphics) {
        if (nativeGraphics instanceof PoseStack) {
            render((PoseStack) nativeGraphics);
            return;
        }
        throw new IllegalStateException("nativeGraphics is not a PoseStack on this branch: " + nativeGraphics.getClass().getName());
    }

    /**
     * 处理叠加层上的通知点击
     *
     * @return 是否已消费
     */
    @Environment(EnvType.CLIENT)
    public boolean tryHandleHudClick(double guiMouseX, double guiMouseY, int button) {
        if (button != 0) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        for (int idx = frameDrawOrder.size() - 1; idx >= 0; idx--) {
            Notification n = frameDrawOrder.get(idx);
            if (n.finished() || n.scheduledTime() > currentTime) {
                continue;
            }
            if (!n.containsPoint(guiMouseX, guiMouseY)) {
                continue;
            }
            if (n.isCloseHit(guiMouseX, guiMouseY)) {
                n.dismiss();
                return true;
            }
            Style st = n.styleAtTextPoint(guiMouseX, guiMouseY);
            if (st != null && NotificationStyleInteractionHelper.tryClickStyle(st)) {
                return true;
            }
            if (n.isBodyHit(guiMouseX, guiMouseY)) {
                BaniraClientRuntime.setScreen(new NotificationLogScreen(new NotificationLogScreen.Args()
                        .parentScreen(BaniraClientRuntime.currentScreen())
                        .selectLogEntryId(n.logEntryId())));
                return true;
            }
        }
        return false;
    }

    /**
     * 无 GUI 时于客户端刻检测鼠标左键按下（与 {@link #render} 使用同一 {@link #frameDrawOrder}）。
     */
    @Environment(EnvType.CLIENT)
    public void tickOutOfScreenClick() {
        if (BaniraClientRuntime.currentScreen() != null) {
            return;
        }
        boolean down = BaniraClientRuntime.leftMouseDown();
        if (down && !prevLeftDown) {
            tryHandleHudClick(BaniraClientRuntime.scaledMouseX(), BaniraClientRuntime.scaledMouseY(), 0);
        }
        prevLeftDown = down;
    }

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
