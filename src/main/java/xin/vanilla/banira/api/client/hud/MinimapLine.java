package xin.vanilla.banira.api.client.hud;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.Objects;
import java.util.function.Supplier;

public final class MinimapLine {
    private final Identifier id;
    private final Component displayName;
    private final Supplier<Component> text;
    private final Supplier<EnumNotificationHudHost> host;
    private final int order;

    public MinimapLine(Identifier id, Component displayName, Supplier<Component> text,
                       Supplier<EnumNotificationHudHost> host, int order) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName").copy();
        this.text = Objects.requireNonNull(text, "text");
        this.host = Objects.requireNonNull(host, "host");
        this.order = order;
    }

    public Identifier id() {
        return id;
    }

    public Component displayName() {
        return displayName.copy();
    }

    public Component text() {
        return text.get();
    }

    public EnumNotificationHudHost host() {
        return host.get();
    }

    public int order() {
        return order;
    }
}
