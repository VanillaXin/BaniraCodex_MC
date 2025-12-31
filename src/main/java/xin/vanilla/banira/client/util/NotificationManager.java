package xin.vanilla.banira.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumMoveType;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;

import java.util.*;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public final class NotificationManager {
    private static final Logger LOGGER = LogManager.getLogger();

    @Data
    @Accessors(chain = true)
    public static class Notification {

        // region 样式配置
        /**
         * 内边距
         */
        private double padding = 5;
        /**
         * 外边距
         */
        private double margin = 5;
        /**
         * 背景颜色
         */
        private Color bgColor = Color.argb(BaniraColorConfig.winter().popupBg());
        /**
         * 边框颜色
         */
        private Color borderColor = Color.argb(BaniraColorConfig.winter().popupBorder());
        /**
         * 边框大小
         */
        private int borderSize = 1;
        /**
         * 圆角半径
         */
        private int radius = 3;
        /**
         * 内容
         */
        private Component component;
        // endregion

        // region 时间控制
        /**
         * 计划开始渲染时间
         */
        private long scheduledTime = System.currentTimeMillis();
        /**
         * 实际开始渲染时间
         */
        private long startTime = -1;
        /**
         * 持续显示时间，非总显示时间<br/>
         * 总显示时间需在此基础上加上动画时间*2
         */
        private long durationTime = 5000;
        /**
         * 动画时间
         */
        private long animationTime = 600;
        // endregion

        // region 动态速度参数
        /**
         * 最大速度, 像素/秒
         */
        private double maxSpeed = 120.0;
        /**
         * 加速度, 像素/秒²
         */
        private double acceleration = 400.0;
        /**
         * 减速开始距离, 像素
         */
        private double decelerationDistance = 15.0;
        // endregion

        // 位置控制
        private EnumPosition position = EnumPosition.TOP_RIGHT;
        private EnumMoveType animation = EnumMoveType.RIGHT_TO_LEFT;

        // 状态管理
        private int index = 0;
        private boolean finished = false;
        private int lastIndex = 0;
        private double lastY = 0;
        private long lastRenderTime = 0;

        // 缓存字段
        private transient double cachedWidth = -1;
        private transient double cachedHeight = -1;
        private transient Text cachedText;

        private Notification(Component component) {
            this.component = component;
            this.updateCachedText();
        }

        public static Notification ofComponentWithBlack(Component component) {
            return new Notification(component.color(0xFF000000));
        }

        public static Notification ofComponent(Component component) {
            return new Notification(component);
        }

        /**
         * 更新缓存字段
         * 当组件内容发生变化时，需要调用此方法更新缓存字段
         */
        private void updateCachedText() {
            this.cachedText = new Text(this.component());
            this.cachedWidth = AbstractGuiUtils.getTextWidth(this.cachedText()) + this.padding() * 2;
            this.cachedHeight = AbstractGuiUtils.getTextHeight(this.cachedText()) + this.padding() * 2;
        }

        /**
         * 渲染通知
         *
         * @param preInfo     上个通知的布局信息(y, width, height)
         * @param screenInfo  屏幕信息(width, height)
         * @param currentTime 当前时间
         */
        @OnlyIn(Dist.CLIENT)
        private void render(MatrixStack matrixStack, ScreenCoordinate preInfo, ScreenCoordinate screenInfo, long currentTime) {
            if (this.finished) return;
            if (this.startTime < 0) this.startTime = currentTime;
            if (currentTime < this.scheduledTime) return;

            // 计算动画进度
            double progress = this.calculateProgress(currentTime);
            if (progress < 0) {
                this.finished = true;
                return;
            }

            // 位置计算
            ScreenCoordinate coordinate = this.calculatePosition(screenInfo, preInfo);

            // 动画应用
            this.applyAnimationEffect(coordinate, progress);

            // 位置过渡动画处理
            this.handlePositionTransition(coordinate, currentTime);

            // 可见性检查
            if (!this.isVisible(coordinate, screenInfo)) {
                return;
            }

            // 实际渲染
            this.doRender(matrixStack, coordinate);

            // 更新布局上下文
            this.updateLayoutContext(coordinate, preInfo, currentTime);
        }

        /**
         * 计算动画进度
         */
        private double calculateProgress(long currentTime) {
            long elapsed = currentTime - this.startTime();
            long totalTime = this.animationTime() * 2 + this.durationTime();

            if (elapsed > totalTime) return -1;

            if (elapsed < this.animationTime()) {
                return (double) elapsed / this.animationTime();
            } else if (elapsed < this.animationTime() + this.durationTime()) {
                return 1.0;
            } else {
                return 1.0 - (double) (elapsed - this.animationTime() - this.durationTime()) / this.animationTime();
            }
        }

        /**
         * 计算渲染位置
         *
         * @param screenInfo 屏幕信息
         * @param preInfo    上个通知的布局信息
         * @return 当前通知的布局信息
         */
        private ScreenCoordinate calculatePosition(ScreenCoordinate screenInfo, ScreenCoordinate preInfo) {
            ScreenCoordinate info = new ScreenCoordinate();
            switch (this.position()) {
                case TOP_LEFT:
                    info.x(this.margin());
                    info.y(this.margin() + preInfo.y() + preInfo.height());
                    break;
                case TOP_CENTER:
                    info.x((screenInfo.width() - this.cachedWidth()) / 2);
                    info.y(this.margin() + preInfo.y() + preInfo.height());
                    break;
                case TOP_RIGHT:
                    info.x(screenInfo.width() - this.cachedWidth() - this.margin());
                    info.y(this.margin() + preInfo.y() + preInfo.height());
                    break;
                case BOTTOM_LEFT:
                    info.x(this.margin());
                    info.y((preInfo.y() == 0 ? screenInfo.height() : preInfo.y()) - this.margin() - this.cachedHeight());
                    break;
                case BOTTOM_CENTER:
                    info.x((screenInfo.width() - this.cachedWidth()) / 2);
                    info.y((preInfo.y() == 0 ? screenInfo.height() : preInfo.y()) - this.margin() - this.cachedHeight());
                    break;
                case BOTTOM_RIGHT:
                    info.x(screenInfo.width() - this.cachedWidth() - this.margin());
                    info.y((preInfo.y() == 0 ? screenInfo.height() : preInfo.y()) - this.margin() - this.cachedHeight());
                    break;
                case CENTER:
                    info.x((screenInfo.width() - this.cachedWidth()) / 2);
                    info.y((screenInfo.height() - this.cachedHeight()) / 2);
                    break;
                default:
                    info.x(this.margin());
                    info.y(this.margin());
            }
            return info;
        }

        /**
         * 应用动画效果
         *
         * @param coordinate 当前通知的布局信息
         * @param progress   动画进度
         */
        private void applyAnimationEffect(ScreenCoordinate coordinate, double progress) {
            switch (this.animation()) {
                case RIGHT_TO_LEFT:
                    coordinate.x(coordinate.x() + this.cachedWidth() * (1 - progress));
                    break;
                case LEFT_TO_RIGHT:
                    coordinate.x(coordinate.x() - this.cachedWidth() * (1 - progress));
                    break;
                case TOP_TO_BOTTOM:
                    coordinate.y(coordinate.y() - this.cachedHeight() * (1 - progress));
                    break;
                case BOTTOM_TO_TOP:
                    coordinate.y(coordinate.y() + this.cachedHeight() * (1 - progress));
                    break;
                case FADE_IN:
                    // 取得背景颜色的alpha通道
                    int a = this.bgColor().alpha();
                    int alpha = (int) ((a == 0 ? 0xFF : a) * progress);
                    this.bgColor().setAlpha(alpha);
                    break;
            }
        }

        /**
         * 处理位置过渡动画（使用缓动函数和速度衰减）
         *
         * @param coordinate  当前坐标信息
         * @param currentTime 当前时间戳(ms)
         */
        private void handlePositionTransition(ScreenCoordinate coordinate, long currentTime) {
            if (this.lastIndex() > 0 && this.index() == 0 && this.lastY() != coordinate.y()) {
                final double targetY = coordinate.y();
                final double currentY = this.lastY();
                final double deltaY = targetY - currentY;

                // 时间差（second）
                final double deltaTime = (currentTime - this.lastRenderTime()) / 1000.0;
                if (deltaTime <= 0) return;

                // 使用缓动函数计算速度
                final double distance = Math.abs(deltaY);
                final double direction = Math.signum(deltaY);

                // 当前速度
                double currentSpeed = Math.min(this.maxSpeed(), Math.sqrt(2 * this.acceleration() * distance));

                // 接近目标时开始减速
                if (distance < this.decelerationDistance()) {
                    currentSpeed *= easeOutQuad(distance / this.decelerationDistance());
                }

                // 位移
                double movement = currentSpeed * deltaTime * direction;
                double newY = currentY + movement;

                // 防止过冲
                if ((direction > 0 && newY > targetY) || (direction < 0 && newY < targetY)) {
                    newY = targetY;
                }

                // 根据位置类型限制坐标
                switch (this.position()) {
                    case TOP_LEFT:
                    case TOP_CENTER:
                    case TOP_RIGHT:
                        coordinate.y(Math.max(targetY, newY));
                        this.index(1);
                        break;
                    case BOTTOM_LEFT:
                    case BOTTOM_CENTER:
                    case BOTTOM_RIGHT:
                        coordinate.y(Math.min(targetY, newY));
                        this.index(1);
                        break;
                }

                // 到达目标后更新状态
                if (Math.abs(coordinate.y() - targetY) < 0.1) {
                    coordinate.y(targetY);
                }
            }
        }

        /**
         * 二次缓出函数（用于减速阶段）
         *
         * @param t 标准化进度 [0,1]
         * @return 缓动系数
         */
        private double easeOutQuad(double t) {
            return 1 - (1 - t) * (1 - t);
        }

        /**
         * 判断是否可见
         *
         * @param info       当前通知的布局信息
         * @param screenInfo 屏幕信息
         * @return 是否可见
         */
        private boolean isVisible(ScreenCoordinate info, ScreenCoordinate screenInfo) {
            return info.x() + this.cachedWidth() > 0 &&
                    info.x() < screenInfo.width() &&
                    info.y() + this.cachedHeight() > 0 &&
                    info.y() < screenInfo.height();
        }

        /**
         * 执行渲染
         *
         * @param coordinate 当前通知的布局信息
         */
        private void doRender(MatrixStack matrixStack, ScreenCoordinate coordinate) {
            AbstractGuiUtils.renderByDepth(matrixStack, EnumRenderDepth.POPUP_TIPS, (stack) -> {
                ShapeDrawArgs rect = ShapeDrawArgs.rect(stack, (float) coordinate.x(), (float) coordinate.y(), (float) this.cachedWidth(), (float) this.cachedHeight(), this.bgColor().argb());
                rect.rect().radius(this.radius());
                BaseShapeWidget.drawShape(rect);

                ShapeDrawArgs rectBorder = ShapeDrawArgs.rect(stack, (float) coordinate.x(), (float) coordinate.y(), (float) this.cachedWidth(), (float) this.cachedHeight(), this.borderColor().argb());
                rectBorder.rect().radius(this.radius()).border(this.borderSize());
                BaseShapeWidget.drawShape(rectBorder);

                FontDrawArgs drawArgs = FontDrawArgs.of(cachedText.stack(stack))
                        .x(coordinate.x() + this.padding()).y(coordinate.y() + this.padding())
                        .position(EnumEllipsisPosition.MIDDLE);
                LabelWidget.drawLimitedText(drawArgs);
            });
        }

        /**
         * 更新布局上下文
         *
         * @param coordinate  当前通知的布局信息
         * @param preLayout   上个通知的布局信息
         * @param currentTime 当前时间
         */
        private void updateLayoutContext(ScreenCoordinate coordinate, ScreenCoordinate preLayout, long currentTime) {
            this.lastY(coordinate.y());
            this.lastIndex(this.index());
            this.lastRenderTime(currentTime);
            preLayout.y(coordinate.y());
            preLayout.width(this.cachedWidth());
            preLayout.height(this.cachedHeight());
        }
    }

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
    public void render(MatrixStack matrixStack) {
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
            ScreenCoordinate preInfo = new ScreenCoordinate().y(pos.name().startsWith("TOP") ? 0 : screenInfo.height()).height(0);

            int i = 0;
            Iterator<Notification> iter = list.iterator();
            while (iter.hasNext()) {
                Notification n = iter.next();

                // 状态过滤
                if (n.finished()) {
                    iter.remove();
                    continue;
                }

                // 位置预计算
                ScreenCoordinate lastInfo = n.calculatePosition(screenInfo, preInfo);

                // 是否可见
                if (this.shouldSkipRendering(pos, lastInfo, screenInfo)) {
                    break;
                }

                // 执行渲染
                n.index(i++).render(matrixStack, preInfo, screenInfo, currentTime);

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
