package xin.vanilla.banira.client.gui.quickaction;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;

import java.util.function.Consumer;

/** 集中创建快捷入口编辑器中需要保持一致的紧凑控件。 */
final class QuickActionWidgets {
    private QuickActionWidgets() {
    }

    static ButtonWidget deleteButton(BaniraScreen screen, String id, int size,
                                     Consumer<ButtonWidget> onClick) {
        ButtonWidget button = new ButtonWidget(screen);
        button.id(id);
        button.presetStyle(ButtonWidget.PresetStyle.MINUS).dangerStyle().padding(3);
        button.onClick(onClick);

        TooltipWidget tooltip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, size, size));
        tooltip.id(id + "_tooltip");
        tooltip.text(BaniraComponent.get().transClientAuto("delete"));
        tooltip.popupAtScreenCoords(true);
        button.addChild(tooltip);
        return button;
    }
}
