package xin.vanilla.banira.common.data;

import lombok.Data;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;


@Data
@Accessors(chain = true, fluent = true)
public class NotificationData {

    // region 样式配置
    private double padding = 5;
    private double margin = 5;
    private Color bgColor = Color.argb(0xE0FFFFFF);
    private Color borderColor = Color.argb(0xFF000000);
    private int borderSize = 1;
    private int radius = 3;
    private Component component;
    // endregion

    // region 时间控制
    private long scheduledTime = System.currentTimeMillis();
    private long durationTime = 5000;
    private long animationTime = 600;
    // endregion

    // region 动态速度参数
    private double maxSpeed = 120.0;
    private double acceleration = 400.0;
    private double decelerationDistance = 15.0;
    // endregion

    // region 位置与动画
    private EnumPosition position = EnumPosition.TOP_RIGHT;
    private EnumMoveType animation = EnumMoveType.AUTO;
    // endregion

    protected NotificationData() {
    }

    protected NotificationData(Component component) {
        this.component = component;
    }

    public static NotificationData of(Component component, EnumPosition position, EnumMoveType animation, long durationTime) {
        NotificationData d = new NotificationData(component);
        d.position(position != null ? position : EnumPosition.TOP_RIGHT);
        d.animation(animation != null ? animation : EnumMoveType.AUTO);
        d.durationTime(durationTime > 0 ? durationTime : 5000L);
        return d;
    }
}
