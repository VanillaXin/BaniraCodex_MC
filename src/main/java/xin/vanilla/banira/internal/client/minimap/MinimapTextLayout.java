package xin.vanilla.banira.internal.client.minimap;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MinimapTextLayout {
    private MinimapTextLayout() {
    }

    public static double nativeY(double afterMap, double mapHeight, double textHeight, int screenHeight) {
        if (afterMap + textHeight <= screenHeight) return Math.max(0, afterMap);
        return Math.max(0, afterMap - mapHeight - textHeight - 6);
    }

    public static List<Component> wrap(Component text, StringSplitter splitter, int width) {
        List<Component> result = new ArrayList<>();
        for (FormattedText line : splitter.splitLines(text, Math.max(1, width), Style.EMPTY)) {
            net.minecraft.network.chat.MutableComponent component = Component.literal("");
            line.visit((style, fragment) -> {
                component.append(Component.literal(fragment).setStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            result.add(component);
        }
        return result;
    }
}
