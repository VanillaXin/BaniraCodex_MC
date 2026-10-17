package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.common.data.Color;
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
                + ";--accent:" + css(theme.accent()) + ";--error:" + css(theme.error()) + ";";
    }
}
