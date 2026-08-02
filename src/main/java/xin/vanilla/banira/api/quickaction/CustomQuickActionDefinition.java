package xin.vanilla.banira.api.quickaction;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/** 可持久化的玩家自定义快捷入口定义。 */
@Data
@Accessors(chain = true)
public class CustomQuickActionDefinition {
    private String id = "";
    private String label = "";
    private boolean enabled = true;
    private QuickActionDisplayMode display = QuickActionDisplayMode.ICON;
    private QuickActionIconType iconType = QuickActionIconType.ITEM;
    private String icon = "minecraft:paper";
    private String keyChord = "";
    private boolean closeBeforeExecution;
    private QuickActionExecutionMode executionMode = QuickActionExecutionMode.PARALLEL;
    private List<CustomQuickActionStep> steps = new ArrayList<>();
    private List<CustomQuickActionMenuItem> contextMenuItems = new ArrayList<>();
}
