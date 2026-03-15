package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumPosition;
import xin.vanilla.banira.client.gui.component.Notification;

import java.util.*;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public final class NotificationManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final EnumMap<EnumPosition, List<Notification>> notifications = new EnumMap<>(EnumPosition.class);
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
        this.notifications.computeIfAbsent(notification.position(), k -> new ArrayList<>()).add(notification);
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
