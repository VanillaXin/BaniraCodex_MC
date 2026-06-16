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
    private int clickCount = 1;
    private boolean doubleClick;
    private boolean clickTracked;

    public static MouseEvent of(double mouseX, double mouseY, int button) {
        return new MouseEvent().mouseX(mouseX).mouseY(mouseY).button(button);
    }

    public static MouseEvent of(double mouseX, double mouseY, int button, MouseClickTracker.Result click) {
        int count = click != null ? click.clickCount() : 1;
        return of(mouseX, mouseY, button)
                .clickCount(Math.max(1, count))
                .doubleClick(click != null && click.doubleClick())
                .clickTracked(click != null);
    }
}
