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
    /**
     * 滚轮原版回调不携带修饰键，由 BaniraScreen 在分发前按当前输入状态补齐。
     */
    private int modifiers;

    public static MouseScrollEvent of(double mouseX, double mouseY, double delta) {
        return of(mouseX, mouseY, delta, 0);
    }

    public static MouseScrollEvent of(double mouseX, double mouseY, double delta, int modifiers) {
        return new MouseScrollEvent().mouseX(mouseX).mouseY(mouseY).delta(delta).modifiers(modifiers);
    }
}
