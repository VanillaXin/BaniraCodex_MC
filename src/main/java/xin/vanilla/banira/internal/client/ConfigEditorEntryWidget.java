package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.gui.widget.BaseWidget;

public interface ConfigEditorEntryWidget {
    BaseWidget getWidget();

    Object getValue();

    void setValue(Object value);

    default boolean isValid() {
        return true;
    }
}
