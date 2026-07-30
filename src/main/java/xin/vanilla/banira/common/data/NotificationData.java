package xin.vanilla.banira.common.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;


@Getter
@Setter
@EqualsAndHashCode
@ToString
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
    /**
     * 语义样式
     */
    private EnumNotificationStyle style = EnumNotificationStyle.NORMAL;
    /**
     * 使用通知类型所属 Mod 的主题解析语义配色。
     */
    private boolean themed = false;
    /**
     * 通知类型
     */
    private String notificationType = NotificationTypeKeys.DEFAULT;
    // endregion

    protected NotificationData() {
    }

    protected NotificationData(Component component) {
        this.component = component;
    }

    public static NotificationData of(Component component, EnumPosition position, EnumMoveType animation, long durationTime) {
        return of(component, position, animation, durationTime, EnumNotificationStyle.NORMAL);
    }

    public static NotificationData of(Component component, EnumPosition position, EnumMoveType animation, long durationTime, EnumNotificationStyle style) {
        return of(component, position, animation, durationTime, style, NotificationTypeKeys.DEFAULT);
    }

    public static NotificationData of(Component component, EnumPosition position, EnumMoveType animation, long durationTime, EnumNotificationStyle style, String notificationType) {
        NotificationData d = new NotificationData(component);
        d.position(position != null ? position : EnumPosition.TOP_RIGHT);
        d.animation(animation != null ? animation : EnumMoveType.AUTO);
        d.durationTime(durationTime > 0 ? durationTime : 5000L);
        d.style(style != null ? style : EnumNotificationStyle.NORMAL);
        d.notificationType(NotificationTypeKeys.normalizeOrDefault(notificationType));
        return d;
    }
}
