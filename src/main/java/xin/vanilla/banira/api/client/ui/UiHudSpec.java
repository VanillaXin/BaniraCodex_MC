package xin.vanilla.banira.api.client.ui;

import java.util.Objects;

public final class UiHudSpec {
    private final String id;
    private final String template;
    private final UiHudBinding binding;
    private final boolean manual;
    private final boolean shared;

    public UiHudSpec(String id, String template, UiHudBinding binding) {
        this(id, template, binding, false);
    }

    public UiHudSpec(String id, String template, UiHudBinding binding, boolean manual) {
        this(id, template, binding, manual, false);
    }

    public UiHudSpec(String id, String template, UiHudBinding binding, boolean manual, boolean shared) {
        this.id = UiPageSpec.validId(id);
        this.template = UiPageSpec.validTemplate(template);
        this.binding = Objects.requireNonNull(binding, "binding");
        this.manual = manual;
        this.shared = shared;
    }

    public String id() {
        return id;
    }

    public String template() {
        return template;
    }

    public UiHudBinding binding() {
        return binding;
    }

    public boolean manual() {
        return manual;
    }

    public boolean shared() {
        return shared;
    }
}
