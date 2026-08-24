package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;

import javax.annotation.Nullable;

public interface ConfigEditorEntryWidget {
    BaseWidget getWidget();

    Object getValue();

    void setValue(Object value);

    @Nullable
    default TooltipWidget tooltipWidget() {
        return null;
    }

    default boolean isValid() {
        return true;
    }
}
