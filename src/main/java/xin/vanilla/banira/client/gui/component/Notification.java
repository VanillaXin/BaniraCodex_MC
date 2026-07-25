package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.data.TransformArgs;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ClientThemeManager;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static xin.vanilla.banira.client.data.BaniraColorToken.TEXT_SECONDARY;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true, fluent = true)
public class Notification extends NotificationData {

    private static final float CLOSE_BTN = 11f;
    private static final float CLOSE_GAP = 4f;

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

    /**
     * 与 {@link xin.vanilla.banira.client.util.NotificationManager} 日志条目对应
     */
    private long logEntryId;

    /**
     * 合并去重：与 {@link #mergeBaseComponent} 序列化一致时的键（类型 + 内容 JSON）
     */
    private String coalesceKey;
    /**
     * 合并前原始文案（不含 “×N” 后缀），用于重复到达时重建展示
     */
    private Component mergeBaseComponent;
    private int coalesceCount = 1;
    /**
     * 最近一次本条作为合并目标被刷新或入队的时间（毫秒）
     */
    private long coalesceLastActivityMs;
    /**
     * 是否由网络包经 {@link #fromData} 应用了客户端主题色
     */
    private boolean themedFromNetwork;

    // 缓存字段
    private transient double cachedWidth = -1;
    private transient double cachedHeight = -1;
    private transient net.minecraft.network.chat.Component vanillaDrawText;
    private transient List<FormattedCharSequence> richDrawLines = new ArrayList<>();
    private transient int richTextMaxLineW;
    private transient int richDefaultTextArgb = 0xFFFFFFFF;
    /**
     * 最近一次绘制在 GUI 坐标下的外接矩形（用于点击检测）
     */
    private transient double hitX;
    private transient double hitY;
    private transient double hitW;
    private transient double hitH;
    private transient double closeBtnLeft;
    private transient double closeBtnTop;
    private transient double closeBtnSize = CLOSE_BTN;
    private transient double bodyLeft;
    private transient double bodyTop;
    private transient double bodyW;
    private transient double bodyH;
    /**
     * 首行文字顶部 GUI Y，用于 {@link #styleAtTextPoint}（文字相对通知垂直居中时与 {@link #bodyTop} 不同）
     */
    private transient double bodyTextTop;

    private Notification(Component component) {
        super(component);
        this.notificationType(NotificationTypeKeys.DEFAULT);
        this.updateRichLayout();
    }

    @Override
    public Notification component(Component component) {
        super.component(component);
        refreshReadableLayout();
        return this;
    }

    @Override
    public Notification bgColor(Color bgColor) {
        super.bgColor(bgColor);
        refreshReadableLayout();
        return this;
    }

    private void refreshReadableLayout() {
        if (component() != null && bgColor() != null) {
            ensureReadableComponentColors();
        }
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
        n.ensureReadableComponentColors();
        return n;
    }

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
        n.notificationType(data.notificationType() != null ? data.notificationType() : NotificationTypeKeys.DEFAULT);
        if (fromNetwork) {
            n.applyClientNotificationStyle(n.style());
        } else {
            n.bgColor(data.bgColor());
            n.borderColor(data.borderColor());
            n.ensureReadableComponentColors();
        }
        n.themedFromNetwork(fromNetwork);
        return n;
    }

    /**
     * 将另一条相同键的通知合并进本条：增加次数、刷新文案与停留时间。
     */
    public void absorbDuplicateFrom(Notification incoming) {
        this.coalesceCount(this.coalesceCount() + 1);
        Component rebuilt = this.mergeBaseComponent().clone();
        if (this.coalesceCount() > 1) {
            rebuilt.append(BaniraComponent.get().literal(" ×" + this.coalesceCount()));
        }
        if (this.themedFromNetwork()) {
            this.component(rebuilt);
            this.applyClientNotificationStyle(this.style());
        } else {
            this.component(rebuilt);
            this.updateRichLayout();
        }
        this.startTime(-1);
        this.scheduledTime(Math.min(this.scheduledTime(), incoming.scheduledTime()));
        this.durationTime(Math.max(this.durationTime(), incoming.durationTime()));
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
        // 白色是 Component 的默认前景色；显式语义色由调用方保留，只在绘制副本上修正对比度。
        if (c.color().isEmpty() || c.color().rgb() == 0xFFFFFF) {
            c.color(Color.argb(textArgb));
        }
        this.component(c);
        this.ensureReadableComponentColors();
    }

    private void ensureReadableComponentColors() {
        this.updateRichLayout();
    }

    private void updateRichLayout() {
        Font font = AbstractGuiUtils.getFont();
        int sw = AbstractGuiUtils.getGuiScaledSize().key();
        int reserve = (int) (padding() * 2 + CLOSE_GAP + CLOSE_BTN + 8);
        int maxTextW = Math.max(40, sw - reserve);
        String lang = Translator.getClientLanguage();
        Component readable = ColorUtils.readableComponentCopy(this.component(), this.bgColor().argb());
        this.vanillaDrawText = readable.toVanilla(lang);
        int sourceTextArgb = readable.color().isEmpty() ? 0xFFFFFFFF : readable.color().argb();
        this.richDefaultTextArgb = ColorUtils.ensureReadableTextArgb(sourceTextArgb, this.bgColor().argb());
        this.richDrawLines = font.split(this.vanillaDrawText, maxTextW);
        this.richTextMaxLineW = 0;
        for (FormattedCharSequence line : this.richDrawLines) {
            this.richTextMaxLineW = Math.max(this.richTextMaxLineW, font.width(line));
        }
        float lineH = font.lineHeight;
        double textH = this.richDrawLines.size() * lineH;
        this.cachedWidth = this.richTextMaxLineW + this.padding() * 2 + CLOSE_GAP + CLOSE_BTN;
        this.cachedHeight = Math.max(textH + this.padding() * 2, CLOSE_BTN + this.padding() * 2);
    }
    public void render(PoseStack stack, ScreenCoordinate preInfo, ScreenCoordinate screenInfo, long currentTime) {
        renderAt(stack, preInfo, screenInfo, currentTime, this.calculatePosition(screenInfo, preInfo));
    }

    /**
     * 使用调用方已计算好的基础坐标渲染，避免通知管理器每帧重复计算位置。
     */
    public void renderAt(PoseStack stack, ScreenCoordinate preInfo, ScreenCoordinate screenInfo, long currentTime, ScreenCoordinate coordinate) {
        if (this.finished) return;
        if (this.startTime < 0) this.startTime = currentTime;
        if (currentTime < this.scheduledTime()) return;

        double progress = this.calculateProgress(currentTime);
        if (progress < 0) {
            this.finished = true;
            return;
        }

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
                int textArgb = drawAlpha < 0xFF
                        ? ColorUtils.applyAlphaToArgb(this.richDefaultTextArgb, drawAlpha)
                        : this.richDefaultTextArgb;

                ShapeDrawArgs rect = ShapeDrawArgs.rect(drawArgs.stack(), x, y, w, h, bgArgb);
                rect.rect().radius(this.radius());
                BaseShapeWidget.drawShape(rect);

                ShapeDrawArgs rectBorder = ShapeDrawArgs.rect(drawArgs.stack(), x, y, w, h, borderArgb);
                rectBorder.rect().radius(this.radius()).border(this.borderSize());
                BaseShapeWidget.drawShape(rectBorder);

                Font font = AbstractGuiUtils.getFont();
                float pad = (float) this.padding();
                float lineH = font.lineHeight;
                float textBlockH = this.richDrawLines.size() * lineH;
                float innerH = h - 2f * pad;
                float textTopLocal = y + pad + Math.max(0f, (innerH - textBlockH) * 0.5f);
                float textX = x + pad;
                float textY = textTopLocal;
                for (FormattedCharSequence line : this.richDrawLines) {
                    font.draw(drawArgs.stack(), line, textX, textY, textArgb);
                    textY += lineH;
                }

                float cbSize = CLOSE_BTN;
                float closeX = x + w - pad - cbSize;
                float closeY = y + pad + Math.max(0f, (innerH - cbSize) * 0.5f);
                BaniraColorConfig theme = ClientThemeManager.getEffectiveTheme();
                int borderRgb = this.borderColor().argb() & 0xFFFFFF;
                int closeBg = (56 << 24) | borderRgb;
                if (drawAlpha < 0xFF) {
                    closeBg = ColorUtils.applyAlphaToArgb(closeBg, drawAlpha);
                }
                int closeIconArgb = theme.color(TEXT_SECONDARY);
                if (drawAlpha < 0xFF) {
                    closeIconArgb = ColorUtils.applyAlphaToArgb(closeIconArgb, drawAlpha);
                }
                ShapeDrawArgs closeBgShape = ShapeDrawArgs.rect(drawArgs.stack(), closeX, closeY, cbSize, cbSize, closeBg);
                closeBgShape.rect().radius(2);
                BaseShapeWidget.drawShape(closeBgShape);
                AbstractGuiUtils.drawNineDotCloseIcon(drawArgs.stack(), closeX, closeY, cbSize, closeIconArgb);

                double ox = coordinate.x();
                double oy = coordinate.y();
                this.hitX = ox;
                this.hitY = oy;
                this.hitW = w;
                this.hitH = h;
                this.closeBtnLeft = ox + w - pad - cbSize;
                this.closeBtnTop = oy + closeY - y;
                this.closeBtnSize = cbSize;
                this.bodyLeft = ox + pad;
                this.bodyTop = oy + pad;
                this.bodyW = w - pad * 2 - CLOSE_GAP - cbSize;
                this.bodyH = innerH;
                this.bodyTextTop = oy + textTopLocal - y;
            });
        });
    }

    public boolean containsPoint(double guiMouseX, double guiMouseY) {
        return guiMouseX >= this.hitX && guiMouseY >= this.hitY
                && guiMouseX < this.hitX + this.hitW && guiMouseY < this.hitY + this.hitH;
    }

    public boolean isCloseHit(double guiMouseX, double guiMouseY) {
        return guiMouseX >= this.closeBtnLeft && guiMouseY >= this.closeBtnTop
                && guiMouseX < this.closeBtnLeft + this.closeBtnSize && guiMouseY < this.closeBtnTop + this.closeBtnSize;
    }

    public boolean isBodyHit(double guiMouseX, double guiMouseY) {
        return guiMouseX >= this.bodyLeft && guiMouseY >= this.bodyTop
                && guiMouseX < this.bodyLeft + this.bodyW && guiMouseY < this.bodyTop + this.bodyH;
    }

    public void dismiss() {
        this.finished = true;
    }

    @Nullable
    public Style styleAtTextPoint(double guiMouseX, double guiMouseY) {
        if (!isBodyHit(guiMouseX, guiMouseY) || richDrawLines.isEmpty()) {
            return null;
        }
        Font font = AbstractGuiUtils.getFont();
        double rx = guiMouseX - this.bodyLeft;
        double ry = guiMouseY - this.bodyTextTop;
        if (rx < 0 || ry < 0 || ry >= this.richDrawLines.size() * font.lineHeight) {
            return null;
        }
        int lineIndex = (int) (ry / font.lineHeight);
        if (lineIndex < 0 || lineIndex >= richDrawLines.size()) {
            return null;
        }
        FormattedCharSequence proc = richDrawLines.get(lineIndex);
        return font.getSplitter().componentStyleAtWidth(proc, (int) rx);
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
