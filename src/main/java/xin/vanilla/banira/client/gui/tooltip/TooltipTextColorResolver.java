package xin.vanilla.banira.client.gui.tooltip;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import xin.vanilla.banira.client.enums.EnumTooltipTextColorPolicy;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.Optional;

/** 在最终绘制前统一解析 Tooltip 的默认色、中性色替换和背景对比度。 */
public final class TooltipTextColorResolver {
    private TooltipTextColorResolver() {
    }

    public static ITextComponent resolve(ITextComponent source, int popupTextArgb, int[] backgroundArgb,
                                    EnumTooltipTextColorPolicy policy) {
        IFormattableTextComponent normalized = new StringTextComponent("");
        source.visit((style, segment) -> {
            if (!segment.isEmpty()) {
                normalized.append(new StringTextComponent(segment).setStyle(
                        resolveDefaultColor(style, popupTextArgb, policy)));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return ColorUtils.readableVanillaComponentCopy(normalized, backgroundArgb);
    }

    private static Style resolveDefaultColor(Style style, int popupTextArgb,
                                             EnumTooltipTextColorPolicy policy) {
        net.minecraft.util.text.Color sourceColor = style.getColor();
        if (sourceColor == null || policy.shouldReplace(sourceColor.getValue())) {
            return style.withColor(net.minecraft.util.text.Color.fromRgb(popupTextArgb & 0x00FFFFFF));
        }
        return style;
    }
}
