package xin.vanilla.banira.client.util;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class NativeTextHit {
    private NativeTextHit() {
    }

    public static Style styleAt(StringSplitter splitter, FormattedCharSequence text, float x) {
        if (!Float.isFinite(x) || x < 0) return null;
        float[] right = {0};
        Style[] hit = {null};
        text.accept((index, style, codepoint) -> {
            right[0] += splitter.stringWidth(FormattedCharSequence.codepoint(codepoint, style));
            if (x < right[0]) {
                hit[0] = style;
                return false;
            }
            return true;
        });
        return hit[0];
    }
}
