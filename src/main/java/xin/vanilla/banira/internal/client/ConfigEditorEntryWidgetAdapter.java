package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigEditorEntryWidgetAdapter implements ConfigEditorEntryWidget {
    private final BaseWidget rowWidget;
    private final Supplier<Object> getter;
    private final Consumer<Object> setter;
    private final Supplier<Boolean> isValidSupplier;

    public ConfigEditorEntryWidgetAdapter(ConfigEntryDescriptor desc, BaseWidget rowWidget, LabelWidget label,
                                          BaseWidget valueWidget, TooltipWidget tooltipWidget,
                                          Supplier<Object> getter, Consumer<Object> setter) {
        this(desc, rowWidget, label, valueWidget, tooltipWidget, getter, setter, null);
    }

    public ConfigEditorEntryWidgetAdapter(ConfigEntryDescriptor desc, BaseWidget rowWidget, LabelWidget label,
                                          BaseWidget valueWidget, TooltipWidget tooltipWidget,
                                          Supplier<Object> getter, Consumer<Object> setter,
                                          Supplier<Boolean> isValidSupplier) {
        this.rowWidget = rowWidget;
        this.getter = getter;
        this.setter = setter;
        this.isValidSupplier = isValidSupplier;
    }

    @Override
    public boolean isValid() {
        return isValidSupplier == null || isValidSupplier.get();
    }

    @Override
    public BaseWidget getWidget() {
        return rowWidget;
    }

    @Override
    public Object getValue() {
        return getter.get();
    }

    @Override
    public void setValue(Object value) {
        setter.accept(value);
    }
}
