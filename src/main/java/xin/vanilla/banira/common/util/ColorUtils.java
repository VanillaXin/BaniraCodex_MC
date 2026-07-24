package xin.vanilla.banira.common.util;

import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMCColor;

import javax.annotation.Nullable;

public final class ColorUtils {
    private static final double MIN_READABLE_CONTRAST = 4.5;

    private ColorUtils() {
    }

    /**
     * 解析ARGB颜色字符串
     * 支持格式：#AARRGGBB、#RRGGBB、0xAARRGGBB、0xRRGGBB
     * 若为6位十六进制（RGB格式），默认alpha为0xFF
     */
    public static int parseArgb(String argb) {
        if (argb == null || argb.isEmpty()) {
            return 0;
        }
        try {
            String cleanHex;
            if (argb.startsWith("#")) {
                cleanHex = argb.substring(1);
            } else if (argb.startsWith("0x") || argb.startsWith("0X")) {
                cleanHex = argb.substring(2);
            } else {
                cleanHex = argb;
            }

            long value = Long.parseLong(cleanHex, 16);

            // 如果是6位十六进制（RGB格式），添加默认alpha值0xFF
            if (cleanHex.length() == 6) {
                return (int) ((0xFFL << 24) | value);
            }
            // 如果是8位十六进制（ARGB格式），直接返回
            return (int) value;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * RGB颜色转换为Minecraft颜色代码
     */
    public static String argbToMinecraftColorString(int color) {
        return "§" + argbToMinecraftColor(color).getCode();
    }

    /**
     * 将ARGB颜色转换为最接近的Minecraft颜色
     */
    public static EnumMCColor argbToMinecraftColor(int color) {
        // 获取 RGB 分量（忽略alpha通道）
        // 无论输入是ARGB还是RGB格式，RGB部分都在低24位
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // 颜色匹配
        double closestDistance = Double.MAX_VALUE;
        // 默认为白色
        EnumMCColor result = EnumMCColor.WHITE;
        for (EnumMCColor mcColor : EnumMCColor.values()) {
            int colorRGB = mcColor.getColor();
            int r = (colorRGB >> 16) & 0xFF;
            int g = (colorRGB >> 8) & 0xFF;
            int b = colorRGB & 0xFF;
            // 加权欧几里得距离计算（人眼对绿色最敏感，蓝色最不敏感）
            double distance = Math.sqrt(2 * Math.pow(red - r, 2) + 4 * Math.pow(green - g, 2) + 3 * Math.pow(blue - b, 2));
            if (distance < closestDistance) {
                closestDistance = distance;
                result = mcColor;
            }
        }
        return result;
    }

    public static int getProbabilityArgb(double probability) {
        int argb = 0xFF000000;
        // 默认不渲染
        if (probability == 1) {
            argb = 0x00FFFFFF;
        }
        // 深灰色，最低级
        else if (probability >= 0.9) {
            argb = 0xEFA9A9A9;
        }
        // 灰色，低级
        else if (probability >= 0.8) {
            argb = 0xEFC0C0C0;
        }
        // 白色，普通
        else if (probability >= 0.7) {
            argb = 0xEFFFFFFF;
        }
        // 亮绿色，良好
        else if (probability >= 0.6) {
            argb = 0xEF32CD32;
        }
        // 深绿色，优秀
        else if (probability >= 0.5) {
            argb = 0xEF228B22;
        }
        // 蓝色，稀有
        else if (probability >= 0.4) {
            argb = 0xEF1E90FF;
        }
        // 深蓝色，稀有
        else if (probability >= 0.3) {
            argb = 0xEF4682B4;
        }
        // 紫色，史诗
        else if (probability >= 0.2) {
            argb = 0xEFA020F0;
        }
        // 金色，传说
        else if (probability >= 0.1) {
            argb = 0xEFFFD700;
        }
        // 橙红色，终极
        else if (probability > 0) {
            argb = 0xEFFF4500;
        }
        return argb;
    }

    /**
     * 获取颜色的亮度（感知亮度）
     * 使用ITU-R BT.709标准权重计算
     *
     * @return 亮度值，范围0.0-1.0
     */
    public static float getBrightness(int rgba) {
        // RGBA格式：R在24-31位，G在16-23位，B在8-15位，A在0-7位
        int r = (rgba >> 24) & 0xFF;
        int g = (rgba >> 16) & 0xFF;
        int b = (rgba >> 8) & 0xFF;
        // 使用ITU-R BT.709标准权重（人眼对绿色最敏感）
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    /**
     * 获取ARGB格式颜色的亮度
     *
     * @return 亮度值，范围0.0-1.0
     */
    public static float getBrightnessFromArgb(int argb) {
        // ARGB格式：A在24-31位，R在16-23位，G在8-15位，B在0-7位
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        // 使用ITU-R BT.709标准权重
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    /**
     * 保留原文字色的色相；对比度不足时，仅向黑色或白色做达到阈值所需的最小混合。
     */
    public static int ensureReadableTextArgb(int textArgb, int backgroundArgb) {
        if (contrastRatio(textArgb, backgroundArgb) >= MIN_READABLE_CONTRAST) {
            return textArgb;
        }
        int darkened = blendUntilReadable(textArgb, backgroundArgb, 0x000000);
        int lightened = blendUntilReadable(textArgb, backgroundArgb, 0xFFFFFF);
        if (darkened == -1) {
            return lightened;
        }
        if (lightened == -1) {
            return darkened;
        }
        return colorDistanceSquared(textArgb, darkened) <= colorDistanceSquared(textArgb, lightened)
                ? darkened
                : lightened;
    }

    /**
     * 创建只用于绘制的可读副本，不污染通知日志中保存的原始颜色。
     */
    public static Component readableComponentCopy(Component component, int backgroundArgb) {
        if (component == null) {
            return null;
        }
        Component result = component.clone();
        adjustComponentColors(result, backgroundArgb);
        return result;
    }

    private static void adjustComponentColors(Component component, int backgroundArgb) {
        if (!component.color().isEmpty()) {
            component.color(Color.argb(ensureReadableTextArgb(component.color().argb(), backgroundArgb)));
        }
        for (Component child : component.getChildren()) {
            adjustComponentColors(child, backgroundArgb);
        }
        for (Component arg : component.getArgs()) {
            if (arg != null) {
                adjustComponentColors(arg, backgroundArgb);
            }
        }
    }

    private static int blendUntilReadable(int textArgb, int backgroundArgb, int targetRgb) {
        int target = (textArgb & 0xFF000000) | targetRgb;
        if (contrastRatio(target, backgroundArgb) < MIN_READABLE_CONTRAST) {
            return -1;
        }
        double low = 0.0;
        double high = 1.0;
        for (int i = 0; i < 24; i++) {
            double mid = (low + high) * 0.5;
            int candidate = blendRgb(textArgb, targetRgb, mid);
            if (contrastRatio(candidate, backgroundArgb) >= MIN_READABLE_CONTRAST) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return blendRgb(textArgb, targetRgb, high);
    }

    private static int blendRgb(int argb, int targetRgb, double amount) {
        int alpha = argb & 0xFF000000;
        int red = blendChannel((argb >> 16) & 0xFF, (targetRgb >> 16) & 0xFF, amount);
        int green = blendChannel((argb >> 8) & 0xFF, (targetRgb >> 8) & 0xFF, amount);
        int blue = blendChannel(argb & 0xFF, targetRgb & 0xFF, amount);
        return alpha | (red << 16) | (green << 8) | blue;
    }

    private static int blendChannel(int from, int to, double amount) {
        return (int) Math.round(from + (to - from) * amount);
    }

    private static long colorDistanceSquared(int firstArgb, int secondArgb) {
        long red = ((firstArgb >> 16) & 0xFF) - ((secondArgb >> 16) & 0xFF);
        long green = ((firstArgb >> 8) & 0xFF) - ((secondArgb >> 8) & 0xFF);
        long blue = (firstArgb & 0xFF) - (secondArgb & 0xFF);
        return red * red + green * green + blue * blue;
    }

    /**
     * 修正旧式 Minecraft 格式码中的低对比度颜色。重置码后没有显式颜色时才补回可读前景色。
     */
    public static String ensureReadableMinecraftFormatting(String text, int backgroundArgb) {
        if (text == null || text.indexOf('\u00A7') < 0) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length() + 4);
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current != '\u00A7' || i + 1 >= text.length()) {
                result.append(current);
                continue;
            }
            char code = Character.toLowerCase(text.charAt(++i));
            if (code == 'r') {
                result.append('\u00A7').append('r');
                if (!hasFollowingLegacyColor(text, i + 1)) {
                    result.append('\u00A7').append(readableLegacyCode(0xFFFFFFFF, backgroundArgb));
                }
                continue;
            }
            Integer color = legacyColorArgb(code);
            result.append('\u00A7').append(color == null
                    ? code
                    : readableLegacyCode(color, backgroundArgb));
        }
        return result.toString();
    }

    /**
     * 检查旧式颜色码是否需要额外阴影；阴影可以保留原格式色，不必将其替换成另一种色相。
     */
    public static boolean hasLowContrastMinecraftFormatting(String text, int backgroundArgb) {
        if (text == null || text.indexOf('\u00A7') < 0) {
            return false;
        }
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '\u00A7') {
                continue;
            }
            Integer color = legacyColorArgb(Character.toLowerCase(text.charAt(++i)));
            if (color != null && contrastRatio(color, backgroundArgb) < MIN_READABLE_CONTRAST) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFollowingLegacyColor(String text, int index) {
        return index + 1 < text.length()
                && text.charAt(index) == '\u00A7'
                && legacyColorArgb(Character.toLowerCase(text.charAt(index + 1))) != null;
    }

    private static char readableLegacyCode(int textArgb, int backgroundArgb) {
        char original = legacyCodeForRgb(textArgb & 0x00FFFFFF);
        if (contrastRatio(textArgb, backgroundArgb) >= MIN_READABLE_CONTRAST) {
            return original;
        }
        int readable = ensureReadableTextArgb(textArgb, backgroundArgb);
        char bestCode = original;
        long bestScore = Long.MAX_VALUE;
        double originalSaturation = saturation(textArgb);
        for (char code : "0123456789abcdef".toCharArray()) {
            Integer candidate = legacyColorArgb(code);
            if (candidate == null || contrastRatio(candidate, backgroundArgb) < MIN_READABLE_CONTRAST) {
                continue;
            }
            long score = colorDistanceSquared(readable, candidate);
            if (originalSaturation >= 0.2 && saturation(candidate) < 0.15) {
                score += 250_000;
            }
            if (score < bestScore) {
                bestScore = score;
                bestCode = code;
            }
        }
        return bestCode;
    }

    private static double saturation(int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return max == 0 ? 0.0 : (max - min) / (double) max;
    }

    private static Integer legacyColorArgb(char code) {
        switch (code) {
            case '0': return 0xFF000000;
            case '1': return 0xFF0000AA;
            case '2': return 0xFF00AA00;
            case '3': return 0xFF00AAAA;
            case '4': return 0xFFAA0000;
            case '5': return 0xFFAA00AA;
            case '6': return 0xFFFFAA00;
            case '7': return 0xFFAAAAAA;
            case '8': return 0xFF555555;
            case '9': return 0xFF5555FF;
            case 'a': return 0xFF55FF55;
            case 'b': return 0xFF55FFFF;
            case 'c': return 0xFFFF5555;
            case 'd': return 0xFFFF55FF;
            case 'e': return 0xFFFFFF55;
            case 'f': return 0xFFFFFFFF;
            default: return null;
        }
    }

    private static char legacyCodeForRgb(int rgb) {
        final String codes = "0123456789abcdef";
        final int[] colors = {
                0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
                0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
                0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
        };
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == rgb) {
                return codes.charAt(i);
            }
        }
        return 'f';
    }

    static double contrastRatio(int firstArgb, int secondArgb) {
        double first = relativeLuminance(firstArgb);
        double second = relativeLuminance(secondArgb);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double relativeLuminance(int argb) {
        double r = linearChannel((argb >> 16) & 0xFF);
        double g = linearChannel((argb >> 8) & 0xFF);
        double b = linearChannel(argb & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linearChannel(int value) {
        double channel = value / 255.0;
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    /**
     * ARGB 颜色反色
     */
    public static int invertArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = 0xFF - ((argb >> 16) & 0xFF);
        int g = 0xFF - ((argb >> 8) & 0xFF);
        int b = 0xFF - (argb & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static boolean isArgbEmpty(int argb) {
        int alpha = (argb >> 24) & 0xFF;
        return alpha == 0x00;
    }

    /**
     * 将外部 alpha（0-255）乘到 ARGB 颜色的透明度通道
     *
     * @param argb  原始 ARGB 颜色
     * @param alpha 外部 alpha 系数，0-255
     * @return 应用 alpha 后的新 ARGB
     */
    public static int applyAlphaToArgb(int argb, int alpha) {
        if (alpha >= 255) return argb;
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int newA = (int) ((a * alpha) / 255.0);
        return (newA << 24) | (r << 16) | (g << 8) | b;
    }

    public static boolean isRgbaEmpty(int rgba) {
        int alpha = rgba & 0xFF;
        return alpha == 0x00;
    }

    /**
     * 柔化颜色
     * 通过调整RGB分量向中间值（128）靠近来实现柔化效果
     */
    public static int softenArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        // 向中间值（128）调整，使颜色更柔和
        int r1 = Math.max(0, Math.min(255, r + (r > 128 ? -30 : 30)));
        int g1 = Math.max(0, Math.min(255, g + (g > 128 ? -30 : 30)));
        int b1 = Math.max(0, Math.min(255, b + (b > 128 ? -30 : 30)));
        return (a << 24) | (r1 << 16) | (g1 << 8) | b1;
    }

    // region Color 类集成方法

    /**
     * 使用Color.parse方法解析并返回Color对象
     */
    public static @Nullable Color parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return null;
        }
        try {
            return Color.parse(colorStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将ARGB颜色值转换为Color对象
     */
    public static Color argbToColor(int argb) {
        return Color.argb(argb);
    }

    /**
     * 将Color对象转换为Minecraft颜色代码字符串
     *
     */
    public static String colorToMinecraftColorString(Color color) {
        if (color == null || color.isEmpty()) {
            return "";
        }
        return argbToMinecraftColorString(color.argb());
    }

    /**
     * 柔化Color对象的颜色
     *
     */
    public static Color softenColor(Color color) {
        if (color == null) {
            return null;
        }
        return Color.argb(softenArgb(color.argb()));
    }

    // endregion

}
