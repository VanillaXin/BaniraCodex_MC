package xin.vanilla.banira.internal.client;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigEditorEntryWidgetAdapter implements ConfigEditorEntryWidget {
    private final BaseWidget rowWidget;
    @Getter
    @Accessors(fluent = true)
    private final LabelWidget labelWidget;
    @Getter
    @Accessors(fluent = true)
    private final TooltipWidget tooltipWidget;
    private final Supplier<Object> getter;
    private final Consumer<Object> setter;
    private final Supplier<Boolean> isValidSupplier;

    public ConfigEditorEntryWidgetAdapter(BaseWidget rowWidget, Supplier<Object> getter, Consumer<Object> setter) {
        this(rowWidget, getter, setter, null);
    }

    public ConfigEditorEntryWidgetAdapter(BaseWidget rowWidget, Supplier<Object> getter, Consumer<Object> setter,
                                          Supplier<Boolean> isValidSupplier) {
        this.rowWidget = rowWidget;
        this.labelWidget = findLabel(rowWidget);
        this.tooltipWidget = findEntryTooltip(rowWidget);
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

    private static LabelWidget findLabel(BaseWidget rowWidget) {
        for (IWidget child : rowWidget.children()) {
            if (child instanceof LabelWidget) {
                return (LabelWidget) child;
            }
        }
        return null;
    }

    private static TooltipWidget findEntryTooltip(BaseWidget rowWidget) {
        for (IWidget child : rowWidget.children()) {
            if (child instanceof TooltipWidget && child.id() != null && child.id().startsWith("tip_")) {
                return (TooltipWidget) child;
            }
        }
        return null;
    }
}
