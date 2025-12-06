package xin.vanilla.banira.common.util;

import xin.vanilla.banira.common.enums.EnumMCColor;

public final class ColorUtils {

    private ColorUtils() {
    }

    public static int argbToHex(String argb) {
        try {
            if (argb.startsWith("#")) {
                return (int) Long.parseLong(argb.substring(1), 16);
            } else if (argb.startsWith("0x")) {
                return (int) Long.parseLong(argb.substring(2), 16);
            } else {
                return (int) Long.parseLong(argb, 16);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * RGB颜色转换为Minecraft颜色代码
     *
     * @param color 颜色值 (ARGB: 0xAARRGGBB 或 RGB: 0xRRGGBB)
     * @return 颜色代码
     */
    public static String argbToMinecraftColorString(int color) {
        return "§" + argbToMinecraftColor(color).getCode();
    }

    public static EnumMCColor argbToMinecraftColor(int color) {
        // 获取 RGB 分量
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
            // 加权欧几里得距离计算
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
     * 获取颜色的亮度
     */
    public static float getBrightness(int rgba) {
        int r = (rgba >> 24) & 0xFF;
        int g = (rgba >> 16) & 0xFF;
        int b = (rgba >> 8) & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    public static boolean isArgbEmpty(int argb) {
        int alpha = (argb >> 24) & 0xFF;
        return alpha == 0x00;
    }

    public static boolean isRgbaEmpty(int rgba) {
        int alpha = rgba & 0xFF;
        return alpha == 0x00;
    }

    /**
     * 柔化颜色
     */
    public static int softenArgb(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        int r1 = Math.max(0, Math.min(255, r + (r > 128 ? -30 : 30)));
        int g1 = Math.max(0, Math.min(255, g + (g > 128 ? -30 : 30)));
        int b1 = Math.max(0, Math.min(255, b + (b > 128 ? -30 : 30)));
        return (a << 24) | (r1 << 16) | (g1 << 8) | b1;
    }

}
