package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 物品 GUI 绘制桥。物品模型、ItemRenderer 和本地玩家都属于版本/加载器敏感实现细节。
 */
public final class BaniraItemRenderBridge {
    private BaniraItemRenderBridge() {
    }

    public static List<Component> tooltipLines(@Nonnull ItemStack stack, boolean advanced) {
        return stack.getTooltipLines(BaniraClientRuntime.localPlayer(), advanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
    }

    public static void renderItem(@Nonnull Font font, @Nonnull ItemStack stack, int x, int y, boolean showCount) {
        renderGuiItemScaled(stack, x, y, 16);
        if (showCount) {
            BaniraClientRuntime.itemRenderer().renderGuiItemDecorations(font, stack, x, y, String.valueOf(stack.getCount()));
        }
    }

    public static void renderScaled(@Nonnull ItemStack stack, int x, int y, int size) {
        renderGuiItemScaled(stack, x, y, size);
    }

    public static void renderFlatIcon(@Nonnull PoseStack pose, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ItemRenderer itemRenderer = BaniraClientRuntime.itemRenderer();
        BakedModel model = itemRenderer.getModel(stack, null, BaniraClientRuntime.localPlayer(), 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        ItemColors itemColors = BaniraClientRuntime.itemColors();
        int tint = itemColors != null ? itemColors.getColor(stack, 0) : -1;
        if (tint != -1) {
            float cr = (float) (tint >> 16 & 255) / 255.0F;
            float cg = (float) (tint >> 8 & 255) / 255.0F;
            float cb = (float) (tint & 255) / 255.0F;
            RenderSystem.setShaderColor(cr, cg, cb, 1f);
        }
        AbstractGuiUtils.blit(pose, TextureAtlas.LOCATION_BLOCKS, x, y, 0, size, size, sprite);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        AbstractGuiUtils.restoreGuiRenderState();
    }

    private static void renderGuiItemScaled(@Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float scale = size / 16f;
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            modelView.translate(x, y, 200f);
            modelView.scale(scale, scale, scale);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            Lighting.setupFor3DItems();
            BaniraClientRuntime.itemRenderer().renderGuiItem(stack, 0, 0);
        } finally {
            Lighting.setupForFlatItems();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            AbstractGuiUtils.restoreGuiRenderState();
        }
    }
}
