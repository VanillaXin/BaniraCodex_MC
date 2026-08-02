package xin.vanilla.banira.client.gui.quickaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.quickaction.*;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** 负责把快捷入口领域对象转换为可校验的 Banira 表单。 */
final class CustomQuickActionEditor {
    private CustomQuickActionEditor() {
    }

    static void openDefinition(Screen parent, CustomQuickActionDefinition source,
                               Consumer<CustomQuickActionDefinition> accepted) {
        CustomQuickActionDefinition draft = copy(source);
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(parent)
                .setHeaderTitle(t("custom_quick_action_edit_title"))
                .addWidget(text("id", "custom_quick_action_id", draft.getId(), false,
                        "[a-z0-9._/-]{1,64}"))
                .addWidget(text("label", "custom_quick_action_label", draft.getLabel(), false, ".{1,80}"))
                .addWidget(dropdown("enabled", "custom_quick_action_enabled",
                        Boolean.toString(draft.isEnabled()), booleanOptions()))
                .addWidget(dropdown("display", "custom_quick_action_display",
                        draft.getDisplay().name(), options(QuickActionDisplayMode.values(), "display")))
                .addWidget(dropdown("iconType", "custom_quick_action_icon_type",
                        draft.getIconType().name(), options(QuickActionIconType.values(), "icon")))
                .addWidget(text("icon", "custom_quick_action_icon_value", draft.getIcon(), true, ".{0,512}"))
                .addWidget(new InputFormScreen.Widget().name("keyChord")
                        .title(t("custom_quick_action_key"))
                        .type(InputFormScreen.WidgetType.KEY_CHORD)
                        .allowEmpty(true).defaultValue(draft.getKeyChord()))
                .addWidget(dropdown("mode", "custom_quick_action_execution_mode",
                        draft.getExecutionMode().name(), options(QuickActionExecutionMode.values(), "execution")))
                .setCallback(result -> {
                    draft.setId(result.value("id").trim())
                            .setLabel(result.value("label").trim())
                            .setEnabled(Boolean.parseBoolean(result.value("enabled")))
                            .setDisplay(valueOf(QuickActionDisplayMode.class, result.value("display"), QuickActionDisplayMode.ICON))
                            .setIconType(valueOf(QuickActionIconType.class, result.value("iconType"), QuickActionIconType.ITEM))
                            .setIcon(result.value("icon").trim())
                            .setKeyChord(result.value("keyChord").trim())
                            .setExecutionMode(valueOf(QuickActionExecutionMode.class, result.value("mode"), QuickActionExecutionMode.PARALLEL));
                    accepted.accept(draft);
                    result.runningResult("");
                });
        Minecraft.getInstance().setScreen(new InputFormScreen(args));
    }

    static void openStep(Screen parent, CustomQuickActionStep source, QuickActionExecutionMode mode,
                         Consumer<CustomQuickActionStep> accepted) {
        CustomQuickActionStep draft = copy(source);
        List<DropdownOption> targets = new ArrayList<>();
        for (String id : CustomQuickActionManager.get().screenIds()) targets.add(new DropdownOption(id));
        if (targets.isEmpty()) targets.add(new DropdownOption(""));

        InputFormScreen.Widget value = new InputFormScreen.Widget().name("value")
                .title(t(draft.getType() == QuickActionStepType.COMMAND
                        ? "custom_quick_action_command" : "custom_quick_action_screen"))
                .defaultValue(draft.getValue()).allowEmpty(false).regex(".{1,1024}");
        if (draft.getType() == QuickActionStepType.SCREEN) {
            value.type(InputFormScreen.WidgetType.DROPDOWN)
                    .dropdownOptionEntries(targets)
                    .dropdownInputMode(DropdownInputMode.EDITABLE);
        }

        QuickActionStepCondition condition = mode == QuickActionExecutionMode.PARALLEL
                ? QuickActionStepCondition.ALWAYS : draft.getCondition();
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(parent)
                .setHeaderTitle(t("custom_quick_action_step_edit_title"))
                .addWidget(dropdown("type", "custom_quick_action_step_type", draft.getType().name(),
                        options(QuickActionStepType.values(), "step_type")))
                .addWidget(dropdown("condition", "custom_quick_action_step_condition", condition.name(),
                        options(QuickActionStepCondition.values(), "condition")).disabled(mode == QuickActionExecutionMode.PARALLEL))
                .addWidget(value)
                .setCallback(result -> {
                    draft.setType(valueOf(QuickActionStepType.class, result.value("type"), draft.getType()))
                            .setCondition(mode == QuickActionExecutionMode.PARALLEL ? QuickActionStepCondition.ALWAYS
                                    : valueOf(QuickActionStepCondition.class, result.value("condition"), QuickActionStepCondition.ALWAYS))
                            .setValue(result.value("value").trim());
                    accepted.accept(draft);
                    result.runningResult("");
                });
        Minecraft.getInstance().setScreen(new InputFormScreen(args));
    }

    static CustomQuickActionDefinition copy(CustomQuickActionDefinition source) {
        CustomQuickActionDefinition copy = new CustomQuickActionDefinition()
                .setId(source == null ? "" : source.getId())
                .setLabel(source == null ? "" : source.getLabel())
                .setEnabled(source == null || source.isEnabled())
                .setDisplay(source == null ? QuickActionDisplayMode.ICON : source.getDisplay())
                .setIconType(source == null ? QuickActionIconType.ITEM : source.getIconType())
                .setIcon(source == null ? "minecraft:paper" : source.getIcon())
                .setKeyChord(source == null ? "" : source.getKeyChord())
                .setExecutionMode(source == null ? QuickActionExecutionMode.PARALLEL : source.getExecutionMode());
        List<CustomQuickActionStep> steps = new ArrayList<>();
        if (source != null && source.getSteps() != null) {
            for (CustomQuickActionStep step : source.getSteps()) if (step != null) steps.add(copy(step));
        }
        return copy.setSteps(steps);
    }

    static CustomQuickActionStep copy(CustomQuickActionStep source) {
        return new CustomQuickActionStep()
                .setType(source == null || source.getType() == null ? QuickActionStepType.COMMAND : source.getType())
                .setCondition(source == null || source.getCondition() == null
                        ? QuickActionStepCondition.ALWAYS : source.getCondition())
                .setValue(source == null || source.getValue() == null ? "" : source.getValue());
    }

    static Text t(String key) {
        return Text.transAuto(BaniraCodex.MODID, key);
    }

    private static InputFormScreen.Widget text(String name, String key, String value,
                                                boolean allowEmpty, String regex) {
        return new InputFormScreen.Widget().name(name).title(t(key)).defaultValue(value)
                .allowEmpty(allowEmpty).regex(regex);
    }

    private static InputFormScreen.Widget dropdown(String name, String key, String value,
                                                    List<DropdownOption> options) {
        return new InputFormScreen.Widget().name(name).title(t(key))
                .type(InputFormScreen.WidgetType.DROPDOWN)
                .dropdownOptionEntries(options)
                .dropdownInputMode(DropdownInputMode.SELECTION_ONLY)
                .defaultValue(value);
    }

    private static List<DropdownOption> booleanOptions() {
        return Arrays.asList(new DropdownOption("true", t("enabled").content()),
                new DropdownOption("false", t("disabled").content()));
    }

    private static List<DropdownOption> options(Enum<?>[] values, String group) {
        List<DropdownOption> result = new ArrayList<>();
        for (Enum<?> value : values) {
            result.add(new DropdownOption(value.name(),
                    t("custom_quick_action_" + group + "_" + value.name().toLowerCase()).content()));
        }
        return result;
    }

    private static <E extends Enum<E>> E valueOf(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
