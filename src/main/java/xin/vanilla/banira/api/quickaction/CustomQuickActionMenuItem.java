package xin.vanilla.banira.api.quickaction;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 自定义快捷入口右键菜单中的无图标动作项。 */
@Data
@Accessors(chain = true)
public class CustomQuickActionMenuItem {
    private String id = UUID.randomUUID().toString();
    private String label = "";
    private boolean closeBeforeExecution;
    private QuickActionExecutionMode executionMode = QuickActionExecutionMode.PARALLEL;
    private List<CustomQuickActionStep> steps = new ArrayList<>();
}
