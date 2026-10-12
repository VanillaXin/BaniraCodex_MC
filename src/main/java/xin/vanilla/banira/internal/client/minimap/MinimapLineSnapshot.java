package xin.vanilla.banira.internal.client.minimap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public final class MinimapLineSnapshot {
    private final ResourceLocation id;
    private final Component text;
    private final int order;

    public MinimapLineSnapshot(ResourceLocation id, Component text, int order) {
        this.id = id;
        this.text = text.copy();
        this.order = order;
    }

    public ResourceLocation id() {
        return id;
    }

    public Component text() {
        return text.copy();
    }

    public int order() {
        return order;
    }
}
