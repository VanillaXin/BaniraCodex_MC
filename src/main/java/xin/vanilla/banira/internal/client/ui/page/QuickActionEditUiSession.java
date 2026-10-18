package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.widget.DropdownInputMode;
import xin.vanilla.banira.client.gui.widget.DropdownOption;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.Consumer;

public final class QuickActionEditUiSession extends NavigationUiSession {
    private final Set<String> toolIds = new HashSet<>();
    private List<DropdownOption> icons;
    private Supplier<String> currentIcon;
    private Consumer<String> selectedIcon;
    public QuickActionEditUiSession(Component title, Supplier<List<Action>> rows, List<Action> tools) {
        super(title, rows, tools);
        for (Action tool : tools) toolIds.add(tool.id());
    }
    public void icons(List<DropdownOption> options, Supplier<String> current, Consumer<String> selected) {
        icons = new ArrayList<>(options); currentIcon = Objects.requireNonNull(current, "current");
        selectedIcon = Objects.requireNonNull(selected, "selected");
    }
    @Override public void bind(UiView view) {
        super.bind(view);
        if (!boundTo(view)) return;
        for (String id : Arrays.asList("details", "icon", "add_command", "add_screen", "add_menu", "save", "cancel", "add")) {
            view.enabled(id, toolIds.contains(id));
        }
        if (icons != null) {
            view.enabled("icon", true);
            view.control("icon", new UiControlSpec(UiControlSpec.Kind.SELECT, 128, "", false, icons, DropdownInputMode.SELECTION_ONLY));
            view.value("icon", UiValue.text(currentIcon.get()));
            view.tooltip("icon", BaniraComponent.get().transClientAuto("custom_quick_action_select_icon"));
            ownBinding(view.onValue("icon", value -> {
                if (boundTo(view) && icons.stream().anyMatch(option -> option.value().equals(value.asText()))) selectedIcon.accept(value.asText());
            }));
        }
    }
}
