package xin.vanilla.banira.client.gui.widget;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.event.CharInputEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;

/**
 * 数值输入 Widget。继承 InputWidget，专为数值输入优化：
 * <ul>
 *   <li>仅允许输入数字、小数点、负号（可选）</li>
 *   <li>支持整数模式（无小数点）</li>
 *   <li>支持滚轮滚动增减数值</li>
 *   <li>支持 min/max 范围限制</li>
 *   <li>支持步进值（step）</li>
 * </ul>
 */
@Accessors(chain = true, fluent = true)
public class NumericInputWidget extends InputWidget {

    @Getter
    @Setter
    private boolean integerOnly = false;

    @Getter
    @Setter
    @Nullable
    private Double minValue;

    @Getter
    @Setter
    @Nullable
    private Double maxValue;

    @Getter
    @Setter
    private double step = 1.0;

    @Getter
    @Setter
    private boolean allowNegative = true;

    /**
     * 小数模式下保留的小数位数。0=整数，-1=自动（整数显示为整数，小数保留原样），>=1=固定位数。默认 2。
     */
    @Getter
    @Setter
    private int decimalPlaces = 2;

    public NumericInputWidget(BaniraScreen screen) {
        super(screen);
    }

    public NumericInputWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    @Override
    protected boolean onCharTyped(CharInputEvent event) {
        if (!focused() || !enabled || !editable()) {
            return false;
        }

        char codePoint = event.codePoint();
        String currentValue = value();
        int cursorPos = cursorPosition();
        int highlightStart = Math.min(cursorPos, highlightPos());
        int highlightEnd = Math.max(cursorPos, highlightPos());
        boolean hasSelection = highlightStart != highlightEnd;

        if (Character.isDigit(codePoint)) {
            return super.onCharTyped(event);
        }

        if (codePoint == '-') {
            if (!allowNegative) return true;
            if (currentValue.isEmpty() && cursorPos == 0) {
                return super.onCharTyped(event);
            }
            if (hasSelection && highlightStart == 0) {
                return super.onCharTyped(event);
            }
            return true;
        }

        if (codePoint == '.' || codePoint == ',') {
            if (integerOnly) return true;
            String effective = hasSelection ? currentValue.substring(0, highlightStart) + currentValue.substring(highlightEnd) : currentValue;
            if (effective.contains(".")) return true;
            return super.onCharTyped(CharInputEvent.of('.', event.modifiers()));
        }

        return true;
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        if (event == null || !canConsumeInput() || renderCoordinate == null) return false;
        double current = parseValue();
        double step = GLFWKey.hasShiftModifier(event.modifiers()) ? this.step * 10 : this.step;
        double delta = event.delta() > 0 ? step : -step;
        double newVal = current + delta;
        if (minValue != null) newVal = Math.max(newVal, minValue);
        if (maxValue != null) newVal = Math.min(newVal, maxValue);
        value(formatValue(newVal));
        return true;
    }

    /**
     * 解析当前文本为数值，无效时返回 0
     */
    public double parseValue() {
        Object v = getParsedValue();
        return v instanceof BigDecimal ? ((BigDecimal) v).doubleValue() : ((Number) v).doubleValue();
    }

    /**
     * 解析当前文本为规范化数值对象。整数返回 Long，小数返回 BigDecimal，无效时返回 BigDecimal.ZERO。
     */
    public Object getParsedValue() {
        String s = value();
        if (StringUtils.isNullOrEmptyEx(s)) return BigDecimal.ZERO;
        s = s.trim();
        if (s.equals("-") || s.equals(".")) return BigDecimal.ZERO;
        try {
            BigDecimal bd = new BigDecimal(s);
            return bd.stripTrailingZeros().scale() <= 0 ? bd.longValue() : bd;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 获取整数值
     */
    public int intValue() {
        return (int) Math.round(parseValue());
    }

    private String formatValue(double v) {
        if (integerOnly) {
            return String.valueOf((int) Math.round(v));
        }
        if (decimalPlaces >= 0) {
            // return String.format("%." + decimalPlaces + "f", v);
            return NumberUtils.toFixedEx(v, decimalPlaces);
        }
        return v == (long) v ? String.valueOf((long) v) : BigDecimal.valueOf(v).toPlainString();
    }

    /**
     * 设置当前数值（内部会格式化为字符串）
     */
    public void setNumericValue(double v) {
        if (minValue != null) v = Math.max(v, minValue);
        if (maxValue != null) v = Math.min(v, maxValue);
        value(formatValue(v));
    }

    /**
     * 增加 step
     */
    public void increment() {
        setNumericValue(parseValue() + step);
    }

    /**
     * 减少 step
     */
    public void decrement() {
        setNumericValue(parseValue() - step);
    }
}
