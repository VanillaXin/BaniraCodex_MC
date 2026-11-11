package xin.vanilla.banira.internal.client.minimap;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MinimapLineSnapshot {
    private final Identifier id;
    private final Component text;
    private final int order;

    public MinimapLineSnapshot(Identifier id, Component text, int order) {
        this.id = id;
        this.text = text.copy();
        this.order = order;
    }

    public Identifier id() {
        return id;
    }

    public Component text() {
        return text.copy();
    }

    public int order() {
        return order;
    }
}
