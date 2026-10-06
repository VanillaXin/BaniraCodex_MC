package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.notification.*;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.banira.internal.client.BaniraClientAccess;
import xin.vanilla.banira.internal.client.NotificationLogStore;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Accessors(fluent = true)
public final class NotificationManager {
    private final EnumMap<EnumPosition, List<Notification>> notifications = new EnumMap<>(EnumPosition.class);
    private final NotificationHistory history = new NotificationHistory();
    private final NotificationMuteState muteState = new NotificationMuteState(System::nanoTime);
    private final AtomicLong nextLogId = new AtomicLong(System.currentTimeMillis());
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
        long logId = nextLogId.incrementAndGet();
        notification.logEntryId(logId);
        appendLog(notification, fromNetwork);
        if (!isTypeHidden(notification.notificationType())) {
            if (NotificationClientDisplay.deliverVanillaIfConfigured(notification.component(), notification.notificationType())) {
                // 已由原版聊天/操作栏展示，不加入浮层
            } else if (!muteState.muted()) {
                long nowMs = System.currentTimeMillis();
                ensureCoalesceFields(notification);
                if (tryMergeOverlayDuplicate(notification, nowMs)) {
                    return;
                }
                if (countActiveNotifications() >= ClientConfig.get().notificationRegions().queueLimit()) return;
                applyBurstStagger(notification, nowMs);
                notification.coalesceLastActivityMs(nowMs);
                this.notifications.computeIfAbsent(notification.position(), k -> new ArrayList<>()).add(notification);
            }
        }
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
        target.absorbDuplicateFrom(incoming, nowMs);
        target.coalesceLastActivityMs(nowMs);
        return true;
    }

    private void applyBurstStagger(Notification n, long nowMs) {
        ClientConfigView cfg = ClientConfig.get();
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
        return history.snapshot();
    }

    public int unreadCount() {
        return history.unreadCount();
    }

    public long historyRevision() {
        return history.revision();
    }

    public synchronized boolean markRead(long id) {
        if (!history.markRead(id)) return false;
        saveLogAsync();
        return true;
    }

    public synchronized boolean markAllRead() {
        if (!history.markAllRead()) return false;
        saveLogAsync();
        return true;
    }

    public boolean overlaysMuted() {
        return muteState.muted();
    }

    public void muteForFiveMinutes() {
        muteState.muteForFiveMinutes();
        long now = System.currentTimeMillis();
        for (List<Notification> group : notifications.values()) {
            for (Notification notification : group) notification.dismissAnimated(now);
        }
        frameDrawOrder.clear();
        frameHoverStyle = null;
    }

    private synchronized void appendLog(Notification notification, boolean fromNetwork) {
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
        history.append(entry, notificationLogMaxEntries());
        saveLogAsync();
    }

    public synchronized void loadLog() {
        List<NotificationLogEntry> loaded = NotificationLogStore.load(notificationLogMaxEntries());
        history.replace(loaded, notificationLogMaxEntries());
        for (NotificationLogEntry entry : loaded) nextLogId.accumulateAndGet(entry.id(), Math::max);
    }

    private static int notificationLogMaxEntries() {
        return Math.max(1, ClientConfig.get().notificationLogMaxEntries());
    }

    private void saveLogAsync() {
        NotificationLogStore.saveAsync(history.snapshot());
    }

    public void render(PoseStack stack) {
        KeyValue<Integer, Integer> scaledSize = BaniraClientAccess.guiScaledSize();
        ScreenCoordinate screenInfo = new ScreenCoordinate()
                .width(scaledSize.key())
                .height(scaledSize.val());
        long currentTime = System.currentTimeMillis();

        frameDrawOrder.clear();
        frameHoverStyle = null;

        KeyValue<Integer, Integer> mouse = InputStateManager.getGuiCursorPos();
        double mx = mouse.key();
        double my = mouse.val();

        for (Map.Entry<EnumPosition, List<Notification>> entry : notifications.entrySet()) {
            entry.getValue().forEach(n -> n.advance(currentTime));
            entry.getValue().removeIf(Notification::finished);
        }

        ClientConfigView.NotificationRegionsView regions = ClientConfig.get().notificationRegions();
        NotificationRegionLayout layout = new NotificationRegionLayout(regions.visibleLimit());
        List<Notification> pending = new ArrayList<>();
        for (List<Notification> group : notifications.values()) pending.addAll(group);
        EnumMap<EnumPosition, Integer> indices = new EnumMap<>(EnumPosition.class);
        for (Notification n : NotificationRegionLayout.admissionOrder(pending, item -> item.lastRenderTime() > 0)) {
            if (n.finished() || n.scheduledTime() > currentTime) continue;
            EnumPosition pos = n.position();
            int[] settings = regionSettings(pos);
            NotificationRegionLayout.Rect region = NotificationRegionLayout.region(pos, scaledSize.key(), scaledSize.val(),
                    settings[0], settings[1], regions.margin());
            n.fitToRegion(region.width, region.height);
            NotificationRegionLayout.Rect target = layout.place(pos, region, (int) Math.ceil(n.cachedWidth()),
                    (int) Math.ceil(n.cachedHeight()), (int) Math.max(0, n.margin()), settings[2]);
            if (target == null) continue;
            int index = indices.getOrDefault(pos, 0);
            indices.put(pos, index + 1);
            frameDrawOrder.add(n);
            n.index(index).renderInRegion(stack, target, region, currentTime);
            layout.follow(pos, region, new NotificationRegionLayout.Rect((int) Math.floor(n.hitX()), (int) Math.floor(n.hitY()),
                    (int) Math.ceil(n.hitW()), (int) Math.ceil(n.hitH())), (int) Math.max(0, n.margin()));
        }

        for (int idx = frameDrawOrder.size() - 1; idx >= 0; idx--) {
            Notification n = frameDrawOrder.get(idx);
            if (n.finished()) {
                continue;
            }
            if (!n.containsPoint(mx, my)) {
                continue;
            }
            if (n.isCloseHit(mx, my)) {
                NotificationStyleInteractionHelper.renderTextTooltip(stack, (int) mx, (int) my,
                        BaniraComponent.get().transClientAuto("notification_mute_five_minutes"),
                        n.notificationTheme());
                break;
            }
            Style st = n.styleAtTextPoint(mx, my);
            if (st != null && st.getHoverEvent() != null) {
                frameHoverStyle = st;
            }
            break;
        }

        if (frameHoverStyle != null) {
            NotificationStyleInteractionHelper.renderHoverTooltip(stack, (int) mx, (int) my,
                    (int) screenInfo.width(), (int) screenInfo.height(), frameHoverStyle);
        }
    }

    /**
     * 处理叠加层上的通知点击
     *
     * @return 是否已消费
     */
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
                muteForFiveMinutes();
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
    public void tickOutOfScreenClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        boolean down = InputStateManager.isMousePressing(GLFWKey.GLFW_MOUSE_BUTTON_LEFT);
        if (down && !prevLeftDown) {
            KeyValue<Integer, Integer> mouse = InputStateManager.getGuiCursorPos();
            tryHandleHudClick(mouse.key(), mouse.val(), 0);
        }
        prevLeftDown = down;
    }

    private static int[] regionSettings(EnumPosition position) {
        ClientConfigView.NotificationRegionsView cfg = ClientConfig.get().notificationRegions();
        switch (position) {
            case TOP_LEFT:
                return new int[]{cfg.topLeft().widthPercent(), cfg.topLeft().heightPercent(), cfg.topLeft().visibleLimit()};
            case TOP_CENTER:
                return new int[]{cfg.topCenter().widthPercent(), cfg.topCenter().heightPercent(), cfg.topCenter().visibleLimit()};
            case TOP_RIGHT:
                return new int[]{cfg.topRight().widthPercent(), cfg.topRight().heightPercent(), cfg.topRight().visibleLimit()};
            case LEFT_CENTER:
                return new int[]{cfg.leftCenter().widthPercent(), cfg.leftCenter().heightPercent(), cfg.leftCenter().visibleLimit()};
            case RIGHT_CENTER:
                return new int[]{cfg.rightCenter().widthPercent(), cfg.rightCenter().heightPercent(), cfg.rightCenter().visibleLimit()};
            case BOTTOM_LEFT:
                return new int[]{cfg.bottomLeft().widthPercent(), cfg.bottomLeft().heightPercent(), cfg.bottomLeft().visibleLimit()};
            case BOTTOM_CENTER:
                return new int[]{cfg.bottomCenter().widthPercent(), cfg.bottomCenter().heightPercent(), cfg.bottomCenter().visibleLimit()};
            case BOTTOM_RIGHT:
                return new int[]{cfg.bottomRight().widthPercent(), cfg.bottomRight().heightPercent(), cfg.bottomRight().visibleLimit()};
            case CENTER:
                return new int[]{cfg.center().widthPercent(), cfg.center().heightPercent(), cfg.center().visibleLimit()};
            default:
                throw new IllegalArgumentException("Unsupported notification position: " + position);
        }
    }
}
