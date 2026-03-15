package xin.vanilla.banira.client.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.JsonUtils;

/**
 * Notification 日志条目，用于持久化与展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true, fluent = true)
public class NotificationLogEntry {

    private long id;
    private long timestamp;
    private String componentJson;
    private String positionName;
    private String animationName;
    private long durationTime;
    private String source; // "network" | "local"

    public Component component() {
        if (componentJson == null || componentJson.isEmpty()) return Component.empty();
        try {
            return Component.deserialize(JsonUtils.parseObject(componentJson));
        } catch (Exception e) {
            return Component.empty();
        }
    }

    public EnumPosition position() {
        return EnumPosition.valueOfEx(positionName);
    }

    public EnumMoveType animation() {
        try {
            return EnumMoveType.valueOf(animationName);
        } catch (Exception e) {
            return EnumMoveType.AUTO;
        }
    }
}
