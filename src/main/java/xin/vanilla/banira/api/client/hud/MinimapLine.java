package xin.vanilla.banira.api.client.hud;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.Objects;
import java.util.function.Supplier;

public final class MinimapLine {
    private final ResourceLocation id;
    private final ITextComponent displayName;
    private final Supplier<ITextComponent> text;
    private final Supplier<EnumNotificationHudHost> host;
    private final int order;

    public MinimapLine(ResourceLocation id, ITextComponent displayName, Supplier<ITextComponent> text,
                       Supplier<EnumNotificationHudHost> host, int order) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName").copy();
        this.text = Objects.requireNonNull(text, "text");
        this.host = Objects.requireNonNull(host, "host");
        this.order = order;
    }

    public ResourceLocation id() {
        return id;
    }

    public ITextComponent displayName() {
        return displayName.copy();
    }

    public ITextComponent text() {
        return text.get();
    }

    public EnumNotificationHudHost host() {
        return host.get();
    }

    public int order() {
        return order;
    }
}
