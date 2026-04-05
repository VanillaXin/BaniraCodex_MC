package xin.vanilla.banira.client.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.experimental.Accessors;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
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

    private final List<Notification> frameDrawOrder = new ArrayList<>();
    private Style frameHoverStyle;

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
            this.notifications.computeIfAbsent(notification.position(), k -> new ArrayList<>()).add(notification);
        }
        appendLog(notification, fromNetwork);
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
        return Collections.unmodifiableList(new ArrayList<>(log));
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
                    obj.addProperty("notificationType", e.notificationType() != null ? e.notificationType() : NotificationTypeKeys.DEFAULT);
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

        frameDrawOrder.clear();
        frameHoverStyle = null;

        double[] mouse = scaledMouse(mc);
        double mx = mouse[0];
        double my = mouse[1];

        for (Map.Entry<EnumPosition, List<Notification>> entry : notifications.entrySet()) {
            entry.getValue().removeIf(Notification::finished);

            EnumPosition pos = entry.getKey();
            List<Notification> list = entry.getValue().stream().filter(n -> n.scheduledTime() <= currentTime).collect(Collectors.toList());

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

                if (i == 0 && (pos == EnumPosition.CENTER || pos == EnumPosition.LEFT_CENTER || pos == EnumPosition.RIGHT_CENTER)) {
                    preInfo.y((screenInfo.height() - n.cachedHeight()) / 2 - n.margin());
                }

                ScreenCoordinate lastInfo = n.calculatePosition(screenInfo, preInfo);

                if (this.shouldSkipRendering(pos, lastInfo, screenInfo)) {
                    break;
                }

                frameDrawOrder.add(n);
                n.index(i++).render(stack, preInfo, screenInfo, currentTime);

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

    private static double[] scaledMouse(Minecraft mc) {
        MainWindow win = mc.getWindow();
        double mx = mc.mouseHandler.xpos() * win.getGuiScaledWidth() / Math.max(1, (double) win.getScreenWidth());
        double my = mc.mouseHandler.ypos() * win.getGuiScaledHeight() / Math.max(1, (double) win.getScreenHeight());
        return new double[]{mx, my};
    }

    /**
     * 处理叠加层上的通知点击
     *
     * @return 是否已消费
     */
    @OnlyIn(Dist.CLIENT)
    public boolean tryHandleHudClick(double guiMouseX, double guiMouseY, int button) {
        if (button != 0) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
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
            if (st != null && NotificationStyleInteractionHelper.tryClickStyle(mc, st)) {
                return true;
            }
            if (n.isBodyHit(guiMouseX, guiMouseY)) {
                mc.setScreen(new NotificationLogScreen(new NotificationLogScreen.Args()
                        .parentScreen(mc.screen)
                        .selectLogEntryId(n.logEntryId())));
                return true;
            }
        }
        return false;
    }

    /**
     * 无 GUI 时于客户端刻检测鼠标左键按下（与 {@link #render} 使用同一 {@link #frameDrawOrder}）。
     */
    @OnlyIn(Dist.CLIENT)
    public void tickOutOfScreenClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        MainWindow win = mc.getWindow();
        boolean down = GLFW.glfwGetMouseButton(win.getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !prevLeftDown) {
            double[] m = scaledMouse(mc);
            tryHandleHudClick(m[0], m[1], 0);
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
