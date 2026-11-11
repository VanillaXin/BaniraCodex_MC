package xin.vanilla.banira.internal.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Matrix3x2fStack;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.ExtractedGuiRenderer;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 物品 GUI 绘制桥。物品模型、ItemRenderer 和本地玩家都属于版本/加载器敏感实现细节。
 */
public final class BaniraItemRenderBridge {
    private BaniraItemRenderBridge() {
    }

    public static List<Component> tooltipLines(@Nonnull ItemStack stack, boolean advanced) {
        return stack.getTooltipLines(Item.TooltipContext.of(BaniraClientRuntime.localPlayer().level()),
                BaniraClientRuntime.localPlayer(),
                advanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
    }

    public static void renderItem(@Nonnull Font font, @Nonnull ItemStack stack, int x, int y, boolean showCount) {
        renderGuiItemScaled(stack, x, y, 16);
        if (showCount) {
            GuiGraphicsExtractor graphics = graphics();
            graphics.pose().pushMatrix();
            try {
                graphics.nextStratum();
                graphics.itemDecorations(font, stack, x, y, String.valueOf(stack.getCount()));
            } finally {
                graphics.pose().popMatrix();
            }
        }
    }

    public static void renderScaled(@Nonnull ItemStack stack, int x, int y, int size) {
        renderGuiItemScaled(stack, x, y, size);
    }

    public static void renderFlatIcon(@Nonnull Matrix3x2fStack pose, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        FlatItemState state = new FlatItemState();
        BaniraClientRuntime.itemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI,
                BaniraClientRuntime.level(), BaniraClientRuntime.localPlayer(), 0);
        if (state.isEmpty()) return;
        var material = state.pickParticleMaterial(RandomSource.create(0L));
        if (material == null) return;
        int color = state.firstLayer == null || state.firstLayer.tintLayers().isEmpty()
                ? -1 : state.firstLayer.tintLayers().getInt(0);
        ExtractedGuiRenderer.withPose(pose, () -> graphics().blitSprite(
                RenderPipelines.GUI_TEXTURED, material.sprite(), x, y, size, size, color));
    }

    private static void renderGuiItemScaled(@Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        GuiGraphicsExtractor graphics = graphics();
        float scale = size / 16f;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            graphics.item(stack, 0, 0);
        } finally {
            graphics.pose().popMatrix();
            AbstractGuiUtils.restoreGuiRenderState();
        }
    }

    private static GuiGraphicsExtractor graphics() {
        return ExtractedGuiRenderer.graphics();
    }

    private static final class FlatItemState extends ItemStackRenderState {
        private LayerRenderState firstLayer;

        @Override
        public LayerRenderState newLayer() {
            LayerRenderState layer = super.newLayer();
            if (firstLayer == null) firstLayer = layer;
            return layer;
        }
    }
}
