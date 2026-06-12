package xin.vanilla.banira.api.client.input;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 统一的鼠标拖拽状态判定器，负责维护起点、累计偏移与开始/结束语义。
 */
public final class BaniraDragTracker {
    private double startX;
    private double startY;
    private double currentX;
    private double currentY;
    private int button = -1;
    private boolean pressed;
    private boolean dragging;

    public void press(double mouseX, double mouseY, int button) {
        this.startX = mouseX;
        this.startY = mouseY;
        this.currentX = mouseX;
        this.currentY = mouseY;
        this.button = button;
        this.pressed = true;
        this.dragging = false;
    }

    public Result drag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!pressed || this.button != button) {
            press(mouseX - deltaX, mouseY - deltaY, button);
        }
        boolean started = !dragging;
        dragging = true;
        currentX = mouseX;
        currentY = mouseY;
        return new Result(true, started, false, this.button, startX, startY, currentX, currentY, deltaX, deltaY);
    }

    public Result release(double mouseX, double mouseY, int button) {
        boolean ended = pressed && this.button == button && dragging;
        Result result = new Result(false, false, ended, button, startX, startY, mouseX, mouseY, 0.0D, 0.0D);
        if (this.button == button) {
            reset();
        }
        return result;
    }

    public void reset() {
        startX = 0.0D;
        startY = 0.0D;
        currentX = 0.0D;
        currentY = 0.0D;
        button = -1;
        pressed = false;
        dragging = false;
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Result {
        private final boolean dragging;
        private final boolean dragStarted;
        private final boolean dragEnded;
        private final int button;
        private final double startX;
        private final double startY;
        private final double mouseX;
        private final double mouseY;
        private final double deltaX;
        private final double deltaY;

        private Result(boolean dragging, boolean dragStarted, boolean dragEnded, int button,
                       double startX, double startY, double mouseX, double mouseY, double deltaX, double deltaY) {
            this.dragging = dragging;
            this.dragStarted = dragStarted;
            this.dragEnded = dragEnded;
            this.button = button;
            this.startX = startX;
            this.startY = startY;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.deltaX = deltaX;
            this.deltaY = deltaY;
        }

        public double totalX() {
            return mouseX - startX;
        }

        public double totalY() {
            return mouseY - startY;
        }
    }
}
