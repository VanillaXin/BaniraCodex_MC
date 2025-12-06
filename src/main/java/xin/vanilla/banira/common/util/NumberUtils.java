package xin.vanilla.banira.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberUtils {

    private NumberUtils() {
    }

    public static int toInt(String s) {
        return toInt(s, 0);
    }

    public static int toInt(String s, int defaultValue) {
        int result = defaultValue;
        if (StringUtils.isNotNullOrEmpty(s)) {
            try {
                result = Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public static long toLong(String s) {
        return toLong(s, 0);
    }

    public static long toLong(String s, long defaultValue) {
        long result = defaultValue;
        if (StringUtils.isNotNullOrEmpty(s)) {
            try {
                result = Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public static float toFloat(String s) {
        return toFloat(s, 0);
    }

    public static float toFloat(String s, float defaultValue) {
        float result = defaultValue;
        if (StringUtils.isNotNullOrEmpty(s)) {
            try {
                result = Float.parseFloat(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public static double toDouble(String s) {
        return toDouble(s, 0);
    }

    public static double toDouble(String s, double defaultValue) {
        double result = defaultValue;
        if (StringUtils.isNotNullOrEmpty(s)) {
            try {
                result = Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public static BigDecimal toBigDecimal(String s) {
        return toBigDecimal(s, BigDecimal.ZERO);
    }

    public static BigDecimal toBigDecimal(String s, BigDecimal defaultValue) {
        BigDecimal result = defaultValue;
        if (StringUtils.isNotNullOrEmpty(s)) {
            try {
                result = new BigDecimal(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    /**
     * 整数转罗马数字
     */
    public static String intToRoman(int num) {
        StringBuilder roman = new StringBuilder();
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                roman.append(symbols[i]);
                num -= values[i];
            }
        }
        return roman.toString();
    }

    /**
     * 转百分数
     */
    public static String toPercent(double num) {
        return toPercent(num, 2);
    }

    /**
     * 转百分数
     */
    public static String toPercent(double num, int scale) {
        return String.format(String.format("%%.%df%%%%", scale), num * 100);
    }

    /**
     * 转百分数
     */
    public static String toPercent(BigDecimal num) {
        return toPercent(num.doubleValue());
    }

    /**
     * 转百分数
     */
    public static String toPercent(BigDecimal num, int scale) {
        return toPercent(num.doubleValue(), scale);
    }

    public static String toFixed(double d, int scale) {
        return new BigDecimal(d).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    public static String toFixedEx(double d, int scale) {
        return toFixed(d, scale).replaceAll("0+$", "").replaceAll("[.]$", "");
    }

    public static String toFixedEx(BigDecimal d, int scale) {
        return d.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replaceAll("0+$", "").replaceAll("[.]$", "");
    }

}
