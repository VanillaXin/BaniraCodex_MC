package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.common.data.Component;

import java.util.Objects;
import java.util.function.Supplier;

public final class PlayerConfigUiPresentation {
    private final String namespace;
    private final Component moduleName, sectionTitle, unit;
    private final Supplier<Component> rangeText;
    private final java.util.Set<String> checkboxes;

    public PlayerConfigUiPresentation(String namespace, Component moduleName, Component sectionTitle,
                                      Component unit, Supplier<Component> rangeText) {
        this(namespace, moduleName, sectionTitle, unit, rangeText, java.util.Collections.emptySet());
    }

    public PlayerConfigUiPresentation(String namespace, Component moduleName, Component sectionTitle,
                                      Component unit, Supplier<Component> rangeText, java.util.Set<String> checkboxes) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
        this.sectionTitle = Objects.requireNonNull(sectionTitle, "sectionTitle");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.rangeText = Objects.requireNonNull(rangeText, "rangeText");
        this.checkboxes = com.google.common.collect.ImmutableSet.copyOf(Objects.requireNonNull(checkboxes, "checkboxes"));
    }

    public String namespace() {
        return namespace;
    }

    public Component moduleName() {
        return moduleName;
    }

    public Component sectionTitle() {
        return sectionTitle;
    }

    public Component unit() {
        return unit;
    }

    public Supplier<Component> rangeText() {
        return rangeText;
    }

    public boolean checkbox(String field) {
        return checkboxes.contains(field);
    }
}
