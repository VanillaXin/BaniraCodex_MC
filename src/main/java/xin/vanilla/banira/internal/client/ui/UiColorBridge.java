package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.util.ColorUtils;
import java.util.Locale;

public final class UiColorBridge {
    private UiColorBridge() { }
    public static String toCss(String nativeColor) { return css(Color.parse(nativeColor).getArgb()); }
    public static String css(int argb) { return Color.argb(argb).toRgbaHexString().toLowerCase(Locale.ROOT); }
    public static String fromCss(String css, java.util.function.ToIntFunction<String> parser) {
        String normalized = css.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("hsla(")) normalized = "hsl(" + normalized.substring(5);
        return Color.argb(parser.applyAsInt(normalized)).toArgbHexString();
    }
    public static String tokens(BaniraColorConfig theme) {
        return "--text:" + css(theme.textPrimary()) + ";--surface:" + css(theme.bgSurface())
                + ";--background:" + css(theme.bgPrimary()) + ";--border:" + css(theme.border())
                + ";--input:" + css(theme.inputBg()) + ";--focus:" + css(theme.borderFocused())
                + ";--button:" + css(theme.buttonBg()) + ";--hover:" + css(theme.buttonBgHover())
                + ";--disabled:" + css(theme.bgDisabled()) + ";--disabled-text:" + css(theme.textDisabled())
                + ";--accent:" + css(theme.accent()) + ";--error:" + css(theme.error()) + ";" + pageTokens(theme);
    }

    private static String pageTokens(BaniraColorConfig theme) {
        boolean dark = ColorUtils.getBrightnessFromArgb(theme.bgPrimary()) < .5f;
        int canvas = tint(dark ? 0xff202423 : 0xfff3f5f4, theme.bgPrimary(), 4);
        int panel = tint(dark ? 0xff292e2b : 0xffffffff, theme.bgSurface(), 2);
        int soft = tint(dark ? 0xff323835 : 0xffedf1ef, theme.bgPrimary(), 4);
        int line = tint(dark ? 0xff505853 : 0xffcbd3ce, theme.border(), 6);
        int ink = ColorUtils.ensureReadableTextArgb(dark ? 0xffe8ede9 : 0xff252d28, new int[]{canvas, panel, soft});
        int action = 0xff000000 | theme.accent() & 0x00ffffff;
        int actionHover = tint(action, dark ? 0xffffffff : 0xff000000, 8);
        int actionText = ColorUtils.ensureReadableTextArgb(ink, new int[]{action, actionHover});
        return "--canvas:" + css(canvas) + ";--panel:" + css(panel) + ";--panel-soft:" + css(soft)
                + ";--line:" + css(line) + ";--ink:" + css(ink)
                + ";--muted:" + css(ColorUtils.ensureReadableTextArgb(dark ? 0xffa6b2aa : 0xff637168, panel))
                + ";--selected:" + css(tint(panel, action, dark ? 18 : 10))
                + ";--action:" + css(action) + ";--action-hover:" + css(actionHover) + ";--action-text:" + css(actionText) + ";";
    }

    private static int tint(int base, int color, int percent) {
        int result = 0xff000000;
        for (int shift : new int[]{0, 8, 16}) {
            result |= (((base >> shift & 255) * (100 - percent) + (color >> shift & 255) * percent) / 100) << shift;
        }
        return result;
    }
}
