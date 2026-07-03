package xin.vanilla.banira.client.gui.event;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 鼠标拖拽事件数据。
 */
@Data
@Accessors(chain = true, fluent = true)
public class MouseDragEvent {
    private double mouseX;
    private double mouseY;
    private int button;
    private double dragX;
    private double dragY;

    public static MouseDragEvent of(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return new MouseDragEvent()
                .mouseX(mouseX).mouseY(mouseY).button(button)
                .dragX(dragX).dragY(dragY);
    }
}
