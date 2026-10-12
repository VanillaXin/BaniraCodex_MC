package xin.vanilla.banira.internal.client.minimap;

import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

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

    public static List<ITextComponent> wrap(ITextComponent text, CharacterManager splitter, int width) {
        List<ITextComponent> result = new ArrayList<>();
        for (ITextProperties line : splitter.splitLines(text, Math.max(1, width), Style.EMPTY)) {
            StringTextComponent component = new StringTextComponent("");
            line.visit((style, fragment) -> {
                component.append(new StringTextComponent(fragment).setStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            result.add(component);
        }
        return result;
    }
}
