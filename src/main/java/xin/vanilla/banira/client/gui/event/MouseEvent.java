package xin.vanilla.banira.client.gui.event;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 鼠标事件数据。
 */
@Data
@Accessors(chain = true, fluent = true)
public class MouseEvent {
    private double mouseX;
    private double mouseY;
    private int button;

    public static MouseEvent of(double mouseX, double mouseY, int button) {
        return new MouseEvent().mouseX(mouseX).mouseY(mouseY).button(button);
    }
}
