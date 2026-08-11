package xin.vanilla.banira.api.quickaction;

import lombok.Data;
import lombok.experimental.Accessors;

/** 单个快捷动作步骤；value 为指令文本、已注册窗口 ID 或窗口完整类名。 */
@Data
@Accessors(chain = true)
public class CustomQuickActionStep {
    private QuickActionStepType type = QuickActionStepType.COMMAND;
    private QuickActionStepCondition condition = QuickActionStepCondition.ALWAYS;
    private String value = "";
}
