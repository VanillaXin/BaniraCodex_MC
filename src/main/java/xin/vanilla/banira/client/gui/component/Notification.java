package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.ColorUtils;


@OnlyIn(Dist.CLIENT)
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true, fluent = true)
public class Notification extends NotificationData {

    // 实际开始渲染时间
    private long startTime = -1;
    // 动画渲染时的临时状态
    private transient int renderAlpha = -1;
    private transient double renderScale = 1.0;
    private transient EnumPosition renderScaleCenter;

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
        super(component);
        this.updateCachedText();
    }

    public static Notification ofComponentWithBlack(Component component) {
        return new Notification(component.color(0xFF000000));
    }

    public static Notification ofComponent(Component component) {
        Component c = component.clone();
        if (c.color().isEmpty() || c.color().rgb() == 0xFFFFFF) {
            c.color(Color.argb(ClientThemeManager.getEffectiveTheme().popupItemText()));
        }
        Notification n = new Notification(c);
        n.bgColor(Color.argb(ClientThemeManager.getEffectiveTheme().popupBg()));
        n.borderColor(Color.argb(ClientThemeManager.getEffectiveTheme().popupBorder()));
        return n;
    }

    /**
     * 从共用数据创建
     */
    public static Notification fromData(NotificationData data) {
        return fromData(data, false);
    }

    /**
     * @param fromNetwork true 时按 {@link EnumNotificationStyle} 应用当前客户端主题
     */
    public static Notification fromData(NotificationData data, boolean fromNetwork) {
        Component comp = data.component() != null ? data.component().clone() : BaniraComponent.get().empty().clone();
        Notification n = new Notification(comp);
        n.position(data.position());
        n.animation(data.animation());
        n.durationTime(data.durationTime());
        n.padding(data.padding());
        n.margin(data.margin());
        n.style(data.style() != null ? data.style() : EnumNotificationStyle.NORMAL);
        n.borderSize(data.borderSize());
        n.radius(data.radius());
        n.scheduledTime(data.scheduledTime());
        n.animationTime(data.animationTime());
        n.maxSpeed(data.maxSpeed());
        n.acceleration(data.acceleration());
        n.decelerationDistance(data.decelerationDistance());
        if (fromNetwork) {
            n.applyClientNotificationStyle(n.style());
        } else {
            n.bgColor(data.bgColor());
            n.borderColor(data.borderColor());
        }
        return n;
    }

    private void applyClientNotificationStyle(EnumNotificationStyle style) {
        BaniraColorConfig t = ClientThemeManager.getEffectiveTheme();
        int bg;
        int border;
        int textArgb;
        switch (style != null ? style : EnumNotificationStyle.NORMAL) {
            case WARNING:
                bg = t.notificationWarningBg();
                border = t.notificationWarningBorder();
                textArgb = t.notificationWarningText();
                break;
            case ERROR:
                bg = t.notificationErrorBg();
                border = t.notificationErrorBorder();
                textArgb = t.notificationErrorText();
                break;
            case SUCCESS:
                bg = t.notificationSuccessBg();
                border = t.notificationSuccessBorder();
                textArgb = t.notificationSuccessText();
                break;
            case NORMAL:
            default:
                bg = t.notificationNormalBg();
                border = t.notificationNormalBorder();
                textArgb = t.notificationNormalText();
                break;
        }
        this.bgColor(Color.argb(bg));
        this.borderColor(Color.argb(border));
        Component c = this.component().clone();
        c.color(Color.argb(textArgb));
        this.component(c);
        this.updateCachedText();
    }

    /**
     * 更新缓存字段
     */
    private void updateCachedText() {
        this.cachedText = new Text(this.component());
        this.cachedWidth = AbstractGuiUtils.getTextWidth(this.cachedText()) + this.padding() * 2;
        this.cachedHeight = AbstractGuiUtils.getTextHeight(this.cachedText()) + this.padding() * 2;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack stack, ScreenCoordinate preInfo, ScreenCoordinate screenInfo, long currentTime) {
        if (this.finished) return;
        if (this.startTime < 0) this.startTime = currentTime;
        if (currentTime < this.scheduledTime()) return;

        double progress = this.calculateProgress(currentTime);
        if (progress < 0) {
            this.finished = true;
            return;
        }

        ScreenCoordinate coordinate = this.calculatePosition(screenInfo, preInfo);
        this.applyAnimationEffect(coordinate, progress);
        this.handlePositionTransition(coordinate, currentTime);

        if (!this.isVisible(coordinate, screenInfo)) {
            return;
        }

        this.doRender(stack, coordinate);
        this.updateLayoutContext(coordinate, preInfo, currentTime);
    }

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

    public ScreenCoordinate calculatePosition(ScreenCoordinate screenInfo, ScreenCoordinate preInfo) {
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
            case LEFT_CENTER:
                info.x(this.margin());
                info.y(this.margin() + preInfo.y() + preInfo.height());
                break;
            case RIGHT_CENTER:
                info.x(screenInfo.width() - this.cachedWidth() - this.margin());
                info.y(this.margin() + preInfo.y() + preInfo.height());
                break;
            case BOTTOM_LEFT:
                info.x(this.margin());
                info.y((Math.min(preInfo.y(), screenInfo.height())) - this.margin() - this.cachedHeight());
                break;
            case BOTTOM_CENTER:
                info.x((screenInfo.width() - this.cachedWidth()) / 2);
                info.y((Math.min(preInfo.y(), screenInfo.height())) - this.margin() - this.cachedHeight());
                break;
            case BOTTOM_RIGHT:
                info.x(screenInfo.width() - this.cachedWidth() - this.margin());
                info.y((Math.min(preInfo.y(), screenInfo.height())) - this.margin() - this.cachedHeight());
                break;
            case CENTER:
                info.x((screenInfo.width() - this.cachedWidth()) / 2);
                info.y(this.margin() + preInfo.y() + preInfo.height());
                break;
            default:
                info.x(this.margin());
                info.y(this.margin());
        }
        return info;
    }

    private EnumMoveType getEffectiveAnimation() {
        if (this.animation() != EnumMoveType.AUTO) return this.animation();
        switch (this.position()) {
            case TOP_LEFT:
            case LEFT_CENTER:
            case BOTTOM_LEFT:
                return EnumMoveType.LEFT_TO_RIGHT;
            case TOP_CENTER:
                return EnumMoveType.TOP_TO_BOTTOM;
            case TOP_RIGHT:
            case RIGHT_CENTER:
            case BOTTOM_RIGHT:
                return EnumMoveType.RIGHT_TO_LEFT;
            case BOTTOM_CENTER:
                return EnumMoveType.BOTTOM_TO_TOP;
            case CENTER:
                return EnumMoveType.FADE_IN;
            default:
                return EnumMoveType.RIGHT_TO_LEFT;
        }
    }

    private void applyAnimationEffect(ScreenCoordinate coordinate, double progress) {
        this.renderAlpha = -1;
        this.renderScale = 1.0;
        this.renderScaleCenter = null;
        switch (getEffectiveAnimation()) {
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
                this.renderAlpha = (int) (0xFF * progress);
                this.renderScaleCenter = this.position();
                break;
            case SCALE_AND_FADE:
                this.renderScale = progress;
                this.renderAlpha = (int) (0xFF * progress);
                this.renderScaleCenter = this.position();
                break;
            default:
                break;
        }
    }

    private void handlePositionTransition(ScreenCoordinate coordinate, long currentTime) {
        if (this.lastIndex() > 0 && this.index() == 0 && this.lastY() != coordinate.y()) {
            final double targetY = coordinate.y();
            final double currentY = this.lastY();
            final double deltaY = targetY - currentY;

            final double deltaTime = (currentTime - this.lastRenderTime()) / 1000.0;
            if (deltaTime <= 0) return;

            final double distance = Math.abs(deltaY);
            final double direction = Math.signum(deltaY);

            double currentSpeed = Math.min(this.maxSpeed(), Math.sqrt(2 * this.acceleration() * distance));

            if (distance < this.decelerationDistance()) {
                currentSpeed *= easeOutQuad(distance / this.decelerationDistance());
            }

            double movement = currentSpeed * deltaTime * direction;
            double newY = currentY + movement;

            if ((direction > 0 && newY > targetY) || (direction < 0 && newY < targetY)) {
                newY = targetY;
            }

            switch (this.position()) {
                case TOP_LEFT:
                case TOP_CENTER:
                case TOP_RIGHT:
                case LEFT_CENTER:
                case RIGHT_CENTER:
                case CENTER:
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

            if (Math.abs(coordinate.y() - targetY) < 0.1) {
                coordinate.y(targetY);
            }
        }
    }

    private double easeOutQuad(double t) {
        return 1 - (1 - t) * (1 - t);
    }

    private boolean isVisible(ScreenCoordinate info, ScreenCoordinate screenInfo) {
        return info.x() + this.cachedWidth() > 0 &&
                info.x() < screenInfo.width() &&
                info.y() + this.cachedHeight() > 0 &&
                info.y() < screenInfo.height();
    }

    private void doRender(PoseStack stack, ScreenCoordinate coordinate) {
        double scale = Math.max(0.01, this.renderScale);
        int alpha = this.renderAlpha < 0 ? 0xFF : (this.renderAlpha < 0xFF ? Math.max(0x01, this.renderAlpha) : 0xFF);
        EnumPosition center = this.renderScaleCenter != null ? this.renderScaleCenter : EnumPosition.TOP_LEFT;

        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.NOTIFICATION, (s) -> {
            TransformArgs args = new TransformArgs(s)
                    .x(coordinate.x()).y(coordinate.y())
                    .width(this.cachedWidth()).height(this.cachedHeight())
                    .scale(scale).alpha(alpha).center(center).blend(alpha < 0xFF || scale != 1.0);
            AbstractGuiUtils.renderByTransform(args, (drawArgs) -> {
                float x = (float) drawArgs.x();
                float y = (float) drawArgs.y();
                float w = (float) drawArgs.width();
                float h = (float) drawArgs.height();
                int drawAlpha = drawArgs.alpha();
                int bgArgb = drawAlpha < 0xFF ? ColorUtils.applyAlphaToArgb(this.bgColor().argb(), drawAlpha) : this.bgColor().argb();
                int borderArgb = drawAlpha < 0xFF ? ColorUtils.applyAlphaToArgb(this.borderColor().argb(), drawAlpha) : this.borderColor().argb();
                int textArgb = drawAlpha < 0xFF ? ColorUtils.applyAlphaToArgb(this.cachedText().colorArgb(), drawAlpha) : this.cachedText().colorArgb();

                ShapeDrawArgs rect = ShapeDrawArgs.rect(drawArgs.stack(), x, y, w, h, bgArgb);
                rect.rect().radius(this.radius());
                BaseShapeWidget.drawShape(rect);

                ShapeDrawArgs rectBorder = ShapeDrawArgs.rect(drawArgs.stack(), x, y, w, h, borderArgb);
                rectBorder.rect().radius(this.radius()).border(this.borderSize());
                BaseShapeWidget.drawShape(rectBorder);

                FontDrawArgs fontArgs = FontDrawArgs.of(cachedText.clone().stack(drawArgs.stack()).color(Color.argb(textArgb)))
                        .x(x + this.padding()).y(y + this.padding())
                        .position(EnumEllipsisPosition.MIDDLE);
                LabelWidget.drawLimitedText(fontArgs);
            });
        });
    }

    private void updateLayoutContext(ScreenCoordinate coordinate, ScreenCoordinate preLayout, long currentTime) {
        this.lastY(coordinate.y());
        this.lastIndex(this.index());
        this.lastRenderTime(currentTime);
        preLayout.y(coordinate.y());
        preLayout.width(this.cachedWidth());
        preLayout.height(this.cachedHeight());
    }
}
