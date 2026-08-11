package xin.vanilla.banira.client.gui.event;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;

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
    private boolean dragging;
    private boolean dragStarted;
    private boolean dragEnded;
    private boolean dragTracked;
    private double dragStartX;
    private double dragStartY;
    private double dragTotalX;
    private double dragTotalY;

    public static MouseDragEvent of(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return new MouseDragEvent()
                .mouseX(mouseX).mouseY(mouseY).button(button)
                .dragX(dragX).dragY(dragY);
    }

    public static MouseDragEvent of(double mouseX, double mouseY, int button, double dragX, double dragY,
                                    BaniraDragTracker.Result drag) {
        return of(mouseX, mouseY, button, dragX, dragY).withDragMetadata(drag);
    }

    /** 写入与公共鼠标事件一致的拖拽语义。 */
    public MouseDragEvent withDragMetadata(BaniraDragTracker.Result drag) {
        if (drag == null) {
            return this;
        }
        this.dragging = drag.dragging();
        this.dragStarted = drag.dragStarted();
        this.dragEnded = drag.dragEnded();
        this.dragStartX = drag.startX();
        this.dragStartY = drag.startY();
        this.dragTotalX = drag.totalX();
        this.dragTotalY = drag.totalY();
        this.dragTracked = true;
        return this;
    }
}
