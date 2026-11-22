package xin.vanilla.banira.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Compact display only; never use the result for accounting or stored quantities.
 */
public final class CompactCount {
    private static final String[] UNITS = {"", "k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"};

    private CompactCount() {
    }

    public static String format(long value) {
        return format(BigDecimal.valueOf(value));
    }

    public static String format(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() > 128) return value;
        try {
            return format(new BigDecimal(value));
        } catch (NumberFormatException invalid) {
            return value;
        }
    }

    public static String format(BigDecimal value) {
        if (value == null || value.signum() == 0) return "0";
        BigDecimal number = value.abs().stripTrailingZeros();
        long exponent = (long) number.precision() - number.scale() - 1;
        String sign = value.signum() < 0 ? "-" : "";
        if (exponent < -3 || exponent >= UNITS.length * 3L) {
            // Avoid expanding untrusted exponent notation into enormous strings.
            return sign + number.unscaledValue().abs().toString().charAt(0) + "e" + exponent;
        }
        int unit = (int) Math.max(0, exponent / 3);
        if (unit == 0) return value.stripTrailingZeros().toPlainString();
        BigDecimal scaled = number.movePointLeft(unit * 3);
        int decimals = scaled.compareTo(BigDecimal.TEN) < 0 ? 2 : scaled.compareTo(BigDecimal.valueOf(100)) < 0 ? 1 : 0;
        return sign + scaled.setScale(decimals, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + UNITS[unit];
    }
}
