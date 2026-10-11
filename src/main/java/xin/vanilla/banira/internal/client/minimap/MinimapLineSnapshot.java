package xin.vanilla.banira.internal.client.minimap;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public final class MinimapLineSnapshot {
    private final ResourceLocation id;
    private final ITextComponent text;
    private final int order;

    public MinimapLineSnapshot(ResourceLocation id, ITextComponent text, int order) {
        this.id = id;
        this.text = text.copy();
        this.order = order;
    }

    public ResourceLocation id() {
        return id;
    }

    public ITextComponent text() {
        return text.copy();
    }

    public int order() {
        return order;
    }
}
