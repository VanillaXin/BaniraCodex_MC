package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
        Minecraft mc = Minecraft.getInstance();
        return stack.getTooltipLines(mc.player, advanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
    }

    public static void renderItem(@Nonnull Font font, @Nonnull ItemStack stack, int x, int y, boolean showCount) {
        Minecraft mc = Minecraft.getInstance();
        renderGuiItemScaled(mc, stack, x, y, 16);
        if (showCount) {
            mc.getItemRenderer().renderGuiItemDecorations(font, stack, x, y, String.valueOf(stack.getCount()));
        }
    }

    public static void renderScaled(@Nonnull ItemStack stack, int x, int y, int size) {
        renderGuiItemScaled(Minecraft.getInstance(), stack, x, y, size);
    }

    public static void renderFlatIcon(@Nonnull PoseStack pose, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, null, mc.player, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        int tint = mc.getItemColors().getColor(stack, 0);
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

    private static void renderGuiItemScaled(@Nonnull Minecraft mc, @Nonnull ItemStack stack, int x, int y, int size) {
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
            mc.getItemRenderer().renderGuiItem(stack, 0, 0);
        } finally {
            Lighting.setupForFlatItems();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            AbstractGuiUtils.restoreGuiRenderState();
        }
    }
}
