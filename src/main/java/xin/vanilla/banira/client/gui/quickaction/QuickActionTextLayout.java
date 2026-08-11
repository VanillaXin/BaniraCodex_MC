package xin.vanilla.banira.client.gui.quickaction;

import java.util.function.ToIntFunction;

/** 为快捷入口列表生成稳定的单行省略文本与完整换行提示。 */
final class QuickActionTextLayout {
    private static final String ELLIPSIS = "...";

    private QuickActionTextLayout() {
    }

    static String ellipsize(String text, int maxWidth, ToIntFunction<String> width) {
        String value = text == null ? "" : text;
        if (value.isEmpty() || width.applyAsInt(value) <= maxWidth) {
            return value;
        }
        if (maxWidth <= 0 || width.applyAsInt(ELLIPSIS) > maxWidth) {
            return ELLIPSIS;
        }
        int prefixLength = fittingPrefixLength(value, maxWidth - width.applyAsInt(ELLIPSIS), width);
        return value.substring(0, prefixLength) + ELLIPSIS;
    }

    static String wrap(String text, int maxWidth, ToIntFunction<String> width) {
        String value = text == null ? "" : text;
        if (value.isEmpty() || maxWidth <= 0) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length() + 16);
        String[] sourceLines = value.split("\\n", -1);
        for (int lineIndex = 0; lineIndex < sourceLines.length; lineIndex++) {
            String remaining = sourceLines[lineIndex];
            while (!remaining.isEmpty() && width.applyAsInt(remaining) > maxWidth) {
                int length = fittingPrefixLength(remaining, maxWidth, width);
                if (length <= 0) {
                    length = 1;
                }
                result.append(remaining, 0, length).append('\n');
                remaining = remaining.substring(length);
            }
            result.append(remaining);
            if (lineIndex + 1 < sourceLines.length) {
                result.append('\n');
            }
        }
        return result.toString();
    }

    private static int fittingPrefixLength(String text, int maxWidth, ToIntFunction<String> width) {
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (width.applyAsInt(text.substring(0, middle)) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }
}
