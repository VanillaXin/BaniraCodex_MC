package xin.vanilla.banira.client.gui.tooltip;

import net.minecraft.network.chat.*;
import xin.vanilla.banira.client.enums.EnumTooltipTextColorPolicy;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.Optional;

/**
 * 在最终绘制前统一解析 Tooltip 的默认色、中性色替换和背景对比度。
 */
public final class TooltipTextColorResolver {
    private TooltipTextColorResolver() {
    }

    public static Component resolve(Component source, int popupTextArgb, int[] backgroundArgb,
                                    EnumTooltipTextColorPolicy policy) {
        MutableComponent normalized = new TextComponent("");
        source.visit((style, segment) -> {
            if (!segment.isEmpty()) {
                normalized.append(new TextComponent(segment).setStyle(
                        resolveDefaultColor(style, popupTextArgb, policy)));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return ColorUtils.readableVanillaComponentCopy(normalized, backgroundArgb);
    }

    private static Style resolveDefaultColor(Style style, int popupTextArgb,
                                             EnumTooltipTextColorPolicy policy) {
        TextColor sourceColor = style.getColor();
        if (sourceColor == null || policy.shouldReplace(sourceColor.getValue())) {
            return style.withColor(TextColor.fromRgb(popupTextArgb & 0x00FFFFFF));
        }
        return style;
    }
}
