package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.common.data.Component;
import java.util.Objects;
import java.util.function.Supplier;

public final class PlayerConfigUiPresentation {
    private final String namespace;
    private final Component moduleName, sectionTitle, unit;
    private final Supplier<Component> rangeText;

    public PlayerConfigUiPresentation(String namespace, Component moduleName, Component sectionTitle,
                                      Component unit, Supplier<Component> rangeText) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.sectionTitle = Objects.requireNonNull(sectionTitle, "sectionTitle");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.rangeText = Objects.requireNonNull(rangeText, "rangeText");
    }

    public String namespace() { return namespace; }
    public Component moduleName() { return moduleName; }
    public Component sectionTitle() { return sectionTitle; }
    public Component unit() { return unit; }
    public Supplier<Component> rangeText() { return rangeText; }
}
