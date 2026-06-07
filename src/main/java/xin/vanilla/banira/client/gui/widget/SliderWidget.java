package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseDragEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

import static xin.vanilla.banira.client.data.BaniraColorToken.*;

/**
 * 数值滑块 Widget。用于在 min～max 范围内选择数值。
 * <ul>
 *   <li>默认仅显示滑块，右键可召唤/收起数值输入框</li>
 *   <li>宽度足够时：右键缩小滑块并在左侧显示输入框；再次右键恢复</li>
 *   <li>宽度较小时：右键直接替换为输入框；再次右键恢复为滑块</li>
 *   <li>支持两种预置样式：条形轨道+圆形滑块 / 方形轨道+嵌入方形滑块</li>
 *   <li>可选在中间显示当前数值</li>
 *   <li>可与外部 NumericInputWidget 绑定（bind），绑定后禁用内联输入</li>
 * </ul>
 */
@Accessors(chain = true, fluent = true)
public class SliderWidget extends BaseWidget {

    /**
     * 滑块预置样式
     */
    public enum SliderStyle {
        /**
         * 条形轨道 + 圆形滑块
         */
        ROUND,
        /**
         * 方形轨道 + 嵌入轨道的方形滑块
         */
        SQUARE
    }

    /**
     * 内联输入显示模式
     */
    private enum InlineInputMode {
        /**
         * 仅滑块
         */
        SLIDER_ONLY,
        /**
         * 输入框 + 滑块并排
         */
        SLIDER_WITH_INPUT,
        /**
         * 仅输入框（宽度不足时替换）
         */
        INPUT_ONLY
    }

    private static final int INLINE_INPUT_W = 60;
    private static final int INLINE_INPUT_GAP = 4;
    /**
     * 宽度低于此值时，右键直接替换为输入框而非并排
     */
    private static final int MIN_WIDTH_FOR_EXPAND = 100;

    @Getter
    @Setter
    private EnumOrientation orientation = EnumOrientation.HORIZONTAL;

    @Getter
    @Setter
    private SliderStyle style = SliderStyle.ROUND;

    @Getter
    @Setter
    private double minValue = 0.0;

    @Getter
    @Setter
    private double maxValue = 100.0;

    @Getter
    @Setter
    private double step = 1.0;

    @Getter
    @Setter
    private double value = 0.0;

    /**
     * 数值显示保留的小数位数。0=整数，-1=自动（整数显示为整数，小数保留原样），>=1=固定位数。默认 2。
     */
    @Getter
    @Setter
    private int decimalPlaces = 2;

    @Override
    public boolean wantsScrollBeforeSiblings() {
        return true;
    }

    /**
     * 是否启用右键召唤内联输入框，默认 true。与外部 bind 时自动设为 false。
     */
    @Getter
    @Setter
    private boolean allowInlineInput = true;

    /**
     * 是否在中间显示当前数值，默认启用
     */
    @Getter
    @Setter
    private boolean showValue = true;

    /**
     * 轨道厚度（像素），ROUND 样式有效
     */
    @Getter
    @Setter
    private int trackThickness = 3;

    /**
     * 滑块（圆形）半径（像素），ROUND 样式有效
     */
    @Getter
    @Setter
    private int thumbRadius = 6;

    /**
     * 方形滑块最小尺寸（像素），SQUARE 样式有效
     */
    @Getter
    @Setter
    private int minThumbSize = 8;

    @Getter
    private double thumbPosition;

    @Getter
    private double thumbSize;

    private double lastThumbTrackSize = Double.NaN;
    private double lastThumbMinValue = Double.NaN;
    private double lastThumbMaxValue = Double.NaN;
    private double lastThumbValue = Double.NaN;
    private int lastThumbRadius = Integer.MIN_VALUE;
    private int lastMinThumbSize = Integer.MIN_VALUE;
    @Nullable
    private SliderStyle lastThumbStyle;

    @Getter
    private boolean dragging;

    @Getter
    private double dragOffset;

    @Getter
    @Setter
    private Consumer<Double> onValueChanged;

    @Getter
    @Setter
    @Nullable
    private Function<Double, String> valueFormatter;

    @Getter
    @Setter
    private int trackColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, SCROLLBAR_BG);

    @Getter
    @Setter
    private int thumbColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, SCROLLBAR_THUMB);

    @Getter
    @Setter
    private int thumbHoverColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, SCROLLBAR_THUMB_HOVER);

    @Getter
    @Setter
    private int valueTextColor = BaniraColorConfig.colorForSeason(EnumSeason.AUTO, TEXT_PRIMARY);

    /**
     * 数值显示区域背景色（半透明，避免与轨道融合）。0 表示根据文字颜色自动选择半透明白/黑
     */
    @Getter
    @Setter
    private int valueBgColor = 0;

    private InlineInputMode inlineInputMode = InlineInputMode.SLIDER_ONLY;
    @Nullable
    private NumericInputWidget inlineInputWidget;
    /**
     * 子组件处理点击时，应获得焦点的目标（供 getFocusTarget 使用）
     */
    @Nullable
    private IWidget lastClickFocusTarget;

    public SliderWidget(BaniraScreen screen) {
        super(screen);
        screen.registerFocusableWidget(this);
    }

    public SliderWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
        screen.registerFocusableWidget(this);
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        trackColor(theme.color(SCROLLBAR_BG)).thumbColor(theme.color(SCROLLBAR_THUMB)).thumbHoverColor(theme.color(SCROLLBAR_THUMB_HOVER))
                .valueTextColor(theme.color(TEXT_PRIMARY));
        if (inlineInputWidget != null) {
            inlineInputWidget.applyTheme(theme);
        }
    }

    @Override
    public boolean handleMouseClick(MouseEvent event) {
        lastClickFocusTarget = null;
        if (event != null && event.button() == GLFWKey.GLFW_MOUSE_BUTTON_RIGHT && allowInlineInput && isMouseInside(event)) {
            toggleInlineInput();
            return true;
        }
        boolean handled = super.handleMouseClick(event);
        if (handled && event != null && event.button() == 0 && inlineInputWidget != null && inlineInputWidget.visible()
                && inlineInputWidget.isMouseInside(event)) {
            lastClickFocusTarget = inlineInputWidget;
        }
        return handled;
    }

    @Override
    public IWidget getFocusTarget() {
        IWidget target = lastClickFocusTarget != null ? lastClickFocusTarget : this;
        lastClickFocusTarget = null;
        return target;
    }

    private void toggleInlineInput() {
        int width = renderCoordinate != null ? (int) renderCoordinate.width() : 0;
        if (inlineInputMode == InlineInputMode.SLIDER_ONLY) {
            if (width >= MIN_WIDTH_FOR_EXPAND) {
                inlineInputMode = InlineInputMode.SLIDER_WITH_INPUT;
            } else {
                inlineInputMode = InlineInputMode.INPUT_ONLY;
            }
            ensureInlineInput();
            syncInlineInputFromSlider();
        } else {
            inlineInputMode = InlineInputMode.SLIDER_ONLY;
            if (inlineInputWidget != null) {
                inlineInputWidget.visible(false);
            }
        }
    }

    private void ensureInlineInput() {
        if (inlineInputWidget != null) return;
        inlineInputWidget = new NumericInputWidget(screen);
        inlineInputWidget.id(id() != null ? id() + "_inline" : "slider_inline");
        inlineInputWidget.text(Text.transAuto(BaniraCodex.MODID, "enter_number"));
        inlineInputWidget.minValue(minValue).maxValue(maxValue).step(step);
        inlineInputWidget.decimalPlaces(decimalPlaces);
        inlineInputWidget.enabled(enabled());
        addChild(inlineInputWidget);
        bindInlineInput();
    }

    private void bindInlineInput() {
        if (inlineInputWidget == null) return;
        inlineInputWidget.onTextChanged(text -> {
            if (enabled()) {
                setValue(inlineInputWidget.parseValue());
            }
        });
    }

    private void syncInlineInputFromSlider() {
        if (inlineInputWidget != null) {
            inlineInputWidget.setNumericValue(value);
            inlineInputWidget.minValue(minValue).maxValue(maxValue).step(step);
            inlineInputWidget.decimalPlaces(decimalPlaces);
        }
    }

    /**
     * 获取内联数值输入框（召唤后存在），用于外部设置错误状态等
     */
    @Nullable
    public NumericInputWidget inlineInputWidget() {
        return inlineInputWidget;
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) {
            return;
        }
        if (renderCoordinate == null) {
            return;
        }

        // region 渲染逻辑

        int x = (int) x();
        int y = (int) y();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();

        // 仅输入框模式：宽度不足时右键切换
        if (inlineInputMode == InlineInputMode.INPUT_ONLY) {
            ensureInlineInput();
            syncInlineInputFromSlider();
            if (inlineInputWidget != null) {
                inlineInputWidget.bounds(new ScreenCoordinate(0, 0, width, height));
                inlineInputWidget.visible(true);
            }
            renderChildren(stack, partialTicks);
            return;
        }

        // 输入框+滑块并排模式
        if (inlineInputMode == InlineInputMode.SLIDER_WITH_INPUT) {
            ensureInlineInput();
            syncInlineInputFromSlider();
            int inputW = Math.min(INLINE_INPUT_W, width - INLINE_INPUT_GAP - 40);
            int sliderW = width - inputW - INLINE_INPUT_GAP;
            if (inlineInputWidget != null) {
                inlineInputWidget.bounds(new ScreenCoordinate(0, 0, inputW, height));
                inlineInputWidget.visible(true);
            }
            double effectiveTrackSize = orientation == EnumOrientation.VERTICAL ? height : sliderW;
            updateThumb(effectiveTrackSize);
            int sliderX = inputW + INLINE_INPUT_GAP;
            if (style == SliderStyle.ROUND) {
                renderRoundStyle(stack, x + sliderX, y, sliderW, height);
            } else {
                renderSquareStyle(stack, x + sliderX, y, sliderW, height);
            }
        } else {
            // 仅滑块模式
            if (inlineInputWidget != null) {
                inlineInputWidget.visible(false);
            }
            updateThumb();
            if (style == SliderStyle.ROUND) {
                renderRoundStyle(stack, x, y, width, height);
            } else {
                renderSquareStyle(stack, x, y, width, height);
            }
            if (showValue) {
                renderValue(stack, x, y, width, height);
            }
        }

        renderChildren(stack, partialTicks);
        // endregion 渲染逻辑
    }

    private void renderRoundStyle(PoseStack stack, int x, int y, int width, int height) {
        int trackT = Math.max(1, Math.min(trackThickness, orientation == EnumOrientation.VERTICAL ? width : height));
        int inset = thumbRadius;

        // 轨道
        float trackRadius = Math.max(1, trackT / 2f);
        if (orientation == EnumOrientation.VERTICAL) {
            int trackX = x + (width - trackT) / 2;
            int trackY = y + inset;
            int trackH = Math.max(0, height - 2 * inset);
            if (trackH > 0) {
                ShapeDrawArgs trackRect = ShapeDrawArgs.rect(stack, trackX, trackY, trackT, trackH, trackColor);
                trackRect.rect().radius(trackRadius).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
                BaseShapeWidget.drawShape(trackRect);
            }
        } else {
            int trackX = x + inset;
            int trackY = y + (height - trackT) / 2;
            int trackW = Math.max(0, width - 2 * inset);
            if (trackW > 0) {
                ShapeDrawArgs trackRect = ShapeDrawArgs.rect(stack, trackX, trackY, trackW, trackT, trackColor);
                trackRect.rect().radius(trackRadius).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
                BaseShapeWidget.drawShape(trackRect);
            }
        }

        // 圆形滑块
        int currentThumbColor = (mouseInside || dragging) ? thumbHoverColor : thumbColor;
        float centerX, centerY;
        if (orientation == EnumOrientation.VERTICAL) {
            centerX = x + width / 2f;
            centerY = (float) (y + thumbPosition + thumbSize / 2.0);
        } else {
            centerX = (float) (x + thumbPosition + thumbSize / 2.0);
            centerY = y + height / 2f;
        }
        ShapeDrawArgs thumbCircle = ShapeDrawArgs.circle(stack, centerX, centerY, thumbRadius, currentThumbColor);
        BaseShapeWidget.drawShape(thumbCircle);
    }

    private void renderSquareStyle(PoseStack stack, int x, int y, int width, int height) {
        // 方形轨道
        ShapeDrawArgs trackRect = ShapeDrawArgs.rect(stack, x, y, width, height, trackColor);
        BaseShapeWidget.drawShape(trackRect);

        // 方形滑块（嵌入轨道内）
        int currentThumbColor = (mouseInside || dragging) ? thumbHoverColor : thumbColor;
        if (orientation == EnumOrientation.VERTICAL) {
            int thumbY = (int) Math.ceil(y + thumbPosition);
            int thumbHeight = (int) Math.max(1, thumbSize);
            ShapeDrawArgs thumbRect = ShapeDrawArgs.rect(stack, x, thumbY, width, thumbHeight, currentThumbColor);
            BaseShapeWidget.drawShape(thumbRect);
        } else {
            int thumbX = (int) Math.ceil(x + thumbPosition);
            int thumbWidth = (int) Math.max(1, thumbSize);
            ShapeDrawArgs thumbRect = ShapeDrawArgs.rect(stack, thumbX, y, thumbWidth, height, currentThumbColor);
            BaseShapeWidget.drawShape(thumbRect);
        }
    }

    private void renderValue(PoseStack stack, int x, int y, int width, int height) {
        Font font = Minecraft.getInstance().font;
        String valueStr = formatDisplayValue(value);
        int textW = font.width(valueStr);
        int textH = font.lineHeight;
        int pad = 2;
        float textX = x + (width - textW) / 2f;
        float textY = y + (height - textH) / 2f;
        int effectiveBgColor = valueBgColor != 0 ? valueBgColor : effectiveValueBgFromTextColor(valueTextColor);
        if (effectiveBgColor != 0) {
            int bgX = (int) textX - pad;
            int bgY = (int) textY - 1;
            int bgW = textW + pad * 2;
            int bgH = textH + 2;
            ShapeDrawArgs bgRect = ShapeDrawArgs.rect(stack, bgX, bgY, bgW, bgH, effectiveBgColor);
            bgRect.rect().radius(2).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
            BaseShapeWidget.drawShape(bgRect);
        }
        font.draw(stack, valueStr, textX, textY, valueTextColor);
    }

    /**
     * 根据文字颜色亮度选择半透明白或半透明黑作为数值背景，保证与文字对比度
     */
    private static int effectiveValueBgFromTextColor(int textColor) {
        int r = (textColor >> 16) & 0xFF;
        int g = (textColor >> 8) & 0xFF;
        int b = textColor & 0xFF;
        double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
        return luminance > 128 ? 0x40000000 : 0x40FFFFFF;
    }

    private String formatDisplayValue(double v) {
        if (valueFormatter != null) {
            String formatted = valueFormatter.apply(v);
            if (StringUtils.isNotNullOrEmpty(formatted)) {
                return formatted;
            }
        }
        if (decimalPlaces >= 0) {
            return NumberUtils.toFixedEx(v, decimalPlaces);
        }
        return v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
    }

    /**
     * 获取当前数值的字符串形式，用于提交等
     */
    public String valueString() {
        return formatDisplayValue(value);
    }

    @Override
    public void update() {
        super.update();
        if (!visible || !enabled) {
            return;
        }
        updateThumb();
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        if (event == null || event.button() != 0 || !enabled || inlineInputMode == InlineInputMode.INPUT_ONLY) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double sliderOffsetX = 0, sliderOffsetY = 0;
        double trackSize = orientation == EnumOrientation.VERTICAL ? height() : width();
        if (inlineInputMode == InlineInputMode.SLIDER_WITH_INPUT) {
            int inputW = Math.min(INLINE_INPUT_W, (int) width() - INLINE_INPUT_GAP - 40);
            int sliderW = (int) width() - inputW - INLINE_INPUT_GAP;
            if (orientation == EnumOrientation.HORIZONTAL) {
                sliderOffsetX = inputW + INLINE_INPUT_GAP;
                trackSize = sliderW;
            } else {
                sliderOffsetY = inputW + INLINE_INPUT_GAP;
                trackSize = height() - sliderOffsetY;
            }
        }
        updateThumb(trackSize);

        double absX = absoluteX() + sliderOffsetX;
        double absY = absoluteY() + sliderOffsetY;
        double relativeClickPos = orientation == EnumOrientation.VERTICAL ? mouseY - absY : mouseX - absX;

        if (relativeClickPos < 0 || relativeClickPos > trackSize) {
            return false;
        }

        double relativeThumbStart = thumbPosition;
        double relativeThumbEnd = relativeThumbStart + thumbSize;

        if (relativeClickPos >= relativeThumbStart && relativeClickPos <= relativeThumbEnd) {
            double relativeThumbCenter = relativeThumbStart + thumbSize / 2.0;
            dragOffset = relativeClickPos - relativeThumbCenter;
            dragging = true;
        } else {
            double availableTrack = trackSize - thumbSize;
            if (availableTrack > 0) {
                double thumbCenterPos = Math.max(thumbSize / 2.0, Math.min(trackSize - thumbSize / 2.0, relativeClickPos));
                double ratio = (thumbCenterPos - thumbSize / 2.0) / availableTrack;
                double newValue = minValue + ratio * (maxValue - minValue);
                setValue(applyStep(newValue));
            }
            dragOffset = 0.0;
            dragging = true;
        }
        return true;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        if (event != null && event.button() == 0 && dragging) {
            dragging = false;
            dragOffset = 0.0;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseDrag(MouseDragEvent event) {
        if (!dragging || event == null || event.button() != 0 || inlineInputMode == InlineInputMode.INPUT_ONLY) {
            return false;
        }

        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        double sliderOffsetX = 0, sliderOffsetY = 0;
        double trackSize = orientation == EnumOrientation.VERTICAL ? height() : width();
        if (inlineInputMode == InlineInputMode.SLIDER_WITH_INPUT) {
            int inputW = Math.min(INLINE_INPUT_W, (int) width() - INLINE_INPUT_GAP - 40);
            if (orientation == EnumOrientation.HORIZONTAL) {
                sliderOffsetX = inputW + INLINE_INPUT_GAP;
                trackSize = width() - sliderOffsetX;
            } else {
                sliderOffsetY = inputW + INLINE_INPUT_GAP;
                trackSize = height() - sliderOffsetY;
            }
        }
        updateThumb(trackSize);

        double absX = absoluteX() + sliderOffsetX;
        double absY = absoluteY() + sliderOffsetY;
        double clickPos = orientation == EnumOrientation.VERTICAL ? mouseY : mouseX;
        double relativePos = orientation == EnumOrientation.VERTICAL ? clickPos - absY : clickPos - absX;

        double availableTrack = trackSize - thumbSize;
        if (availableTrack <= 0) {
            return false;
        }

        double thumbCenterPos = relativePos - dragOffset;
        thumbCenterPos = Math.max(thumbSize / 2.0, Math.min(trackSize - thumbSize / 2.0, thumbCenterPos));

        double ratio = (thumbCenterPos - thumbSize / 2.0) / availableTrack;
        double newValue = minValue + ratio * (maxValue - minValue);
        setValue(applyStep(newValue));
        return true;
    }

    @Override
    public boolean handleMouseScroll(MouseScrollEvent event) {
        if (!visible || !enabled || event == null) {
            return false;
        }
        if (findHandlingChild(child -> child.handleMouseScroll(event)) != null) {
            return true;
        }
        // 仅当获得焦点时响应滚轮，避免滚动列表时误修改
        if (focused()) {
            return onMouseScroll(event);
        }
        return false;
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        if (!enabled || event == null) {
            return false;
        }

        double stepVal = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? step * 10 : step;
        double newValue = value + (event.delta() < 0 ? stepVal : -stepVal);
        setValue(newValue);
        return true;
    }

    /**
     * 将数值对齐到步进
     */
    private double applyStep(double v) {
        if (step <= 0) return v;
        double stepped = Math.round((v - minValue) / step) * step + minValue;
        return Math.max(minValue, Math.min(maxValue, stepped));
    }

    /**
     * 设置数值（会自动裁剪到 min～max 并触发 onValueChanged）
     */
    public void setValue(double value) {
        double newValue = Math.max(minValue, Math.min(maxValue, applyStep(value)));
        if (Math.abs(newValue - this.value) > 1e-9) {
            this.value = newValue;
            updateThumb();
            if (inlineInputWidget != null && inlineInputWidget.visible()) {
                inlineInputWidget.setNumericValue(this.value);
            }
            if (onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
    }

    private void updateThumb() {
        updateThumb(orientation == EnumOrientation.VERTICAL ? height() : width());
    }

    private void updateThumb(double effectiveTrackSize) {
        if (thumbLayoutFresh(effectiveTrackSize)) {
            return;
        }
        rememberThumbLayout(effectiveTrackSize);

        double valueRange = maxValue - minValue;

        if (style == SliderStyle.ROUND) {
            thumbSize = thumbRadius * 2.0;
        } else {
            thumbSize = Math.max(minThumbSize, effectiveTrackSize * 0.1);
            thumbSize = Math.min(thumbSize, effectiveTrackSize);
        }

        if (valueRange <= 0) {
            thumbPosition = 0.0;
            return;
        }

        double availableTrack = Math.max(0, effectiveTrackSize - thumbSize);
        double ratio = (value - minValue) / valueRange;
        thumbPosition = ratio * availableTrack;
    }

    /**
     * render/update 会多次请求 thumb 布局；缓存输入可避免同一帧重复计算。
     */
    private boolean thumbLayoutFresh(double effectiveTrackSize) {
        return Double.compare(lastThumbTrackSize, effectiveTrackSize) == 0
                && Double.compare(lastThumbMinValue, minValue) == 0
                && Double.compare(lastThumbMaxValue, maxValue) == 0
                && Double.compare(lastThumbValue, value) == 0
                && lastThumbRadius == thumbRadius
                && lastMinThumbSize == minThumbSize
                && lastThumbStyle == style;
    }

    private void rememberThumbLayout(double effectiveTrackSize) {
        lastThumbTrackSize = effectiveTrackSize;
        lastThumbMinValue = minValue;
        lastThumbMaxValue = maxValue;
        lastThumbValue = value;
        lastThumbRadius = thumbRadius;
        lastMinThumbSize = minThumbSize;
        lastThumbStyle = style;
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        if (!enabled || !focused) {
            return false;
        }

        double valueRange = maxValue - minValue;
        if (valueRange <= 0) {
            return false;
        }

        double stepVal = net.minecraft.client.gui.screens.Screen.hasShiftDown() ? step * 10 : step;
        boolean handled = false;
        int keyCode = event.keyCode();

        if (orientation == EnumOrientation.VERTICAL) {
            if (keyCode == xin.vanilla.banira.client.data.GLFWKey.GLFW_KEY_UP) {
                setValue(value - stepVal);
                handled = true;
            } else if (keyCode == xin.vanilla.banira.client.data.GLFWKey.GLFW_KEY_DOWN) {
                setValue(value + stepVal);
                handled = true;
            }
        } else {
            if (keyCode == xin.vanilla.banira.client.data.GLFWKey.GLFW_KEY_LEFT) {
                setValue(value - stepVal);
                handled = true;
            } else if (keyCode == xin.vanilla.banira.client.data.GLFWKey.GLFW_KEY_RIGHT) {
                setValue(value + stepVal);
                handled = true;
            }
        }

        return handled;
    }

    /**
     * 将滑块与外部数值输入框双向绑定。绑定后禁用内联输入（右键召唤）。
     *
     * @param slider 滑块
     * @param input  外部数值输入框
     */
    public static void bind(SliderWidget slider, NumericInputWidget input) {
        if (slider == null || input == null) return;

        slider.allowInlineInput(false);

        String inputVal = input.value();
        if (!StringUtils.isNullOrEmptyEx(inputVal) && !inputVal.trim().equals("-") && !inputVal.trim().equals(".")) {
            try {
                double parsed = Double.parseDouble(inputVal.trim());
                slider.setValue(parsed);
            } catch (NumberFormatException e) {
                input.setNumericValue(slider.value());
            }
        } else {
            input.setNumericValue(slider.value());
        }

        if (input.minValue() != null) slider.minValue(input.minValue());
        if (input.maxValue() != null) slider.maxValue(input.maxValue());
        slider.step(input.step());
        slider.decimalPlaces(input.decimalPlaces());

        slider.onValueChanged(v -> {
            if (!input.enabled()) return;
            input.setNumericValue(v);
        });

        input.onTextChanged(text -> {
            if (!slider.enabled()) return;
            slider.setValue(input.parseValue());
        });
    }
}
