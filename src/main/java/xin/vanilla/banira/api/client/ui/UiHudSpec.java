package xin.vanilla.banira.api.client.ui;

import java.util.Objects;

public final class UiHudSpec {
    private final String id;
    private final String template;
    private final UiHudBinding binding;

    public UiHudSpec(String id, String template, UiHudBinding binding) {
        this.id = UiPageSpec.validId(id);
        this.template = UiPageSpec.validTemplate(template);
        this.binding = Objects.requireNonNull(binding, "binding");
    }

    public String id() { return id; }
    public String template() { return template; }
    public UiHudBinding binding() { return binding; }
}
