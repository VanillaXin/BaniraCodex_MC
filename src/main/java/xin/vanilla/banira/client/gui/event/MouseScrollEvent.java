package xin.vanilla.banira.client.gui.event;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 鼠标滚轮事件数据。
 */
@Data
@Accessors(chain = true, fluent = true)
public class MouseScrollEvent {
    private double mouseX;
    private double mouseY;
    private double delta;
    private int modifiers;

    public static MouseScrollEvent of(double mouseX, double mouseY, double delta) {
        return new MouseScrollEvent().mouseX(mouseX).mouseY(mouseY).delta(delta);
    }

    public static MouseScrollEvent of(double mouseX, double mouseY, double delta, int modifiers) {
        return of(mouseX, mouseY, delta).modifiers(modifiers);
    }
}
