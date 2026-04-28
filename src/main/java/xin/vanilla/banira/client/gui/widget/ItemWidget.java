package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ItemUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 物品Widget
 */
@Accessors(chain = true, fluent = true)
public class ItemWidget extends BaseWidget {
    @Getter
    private String itemId;

    @Getter
    private ItemStack itemStack;

    @Getter
    @Setter
    private int itemCount = 1;

    @Getter
    @Setter
    private boolean showCountText = true;

    @Getter
    @Setter
    private boolean enableTooltip = true;

    @Getter
    @Setter
    private boolean vanillaTooltip = false;

    @Getter
    @Setter
    private boolean seasonTooltip = true;

    public ItemWidget(BaniraScreen screen) {
        super(screen);
    }

    public ItemWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public ItemWidget(BaniraScreen screen, ScreenCoordinate bounds, String itemId) {
        super(screen, bounds);
        this.itemId = itemId;
    }

    /**
     * 构造函数
     */
    public ItemWidget(BaniraScreen screen, ScreenCoordinate bounds, ItemStack itemStack) {
        super(screen, bounds);
        this.itemStack = itemStack;
    }

    @Override
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        ItemStack currentItemStack = getCurrentItemStack();
        if (currentItemStack == null || currentItemStack.isEmpty()) {
            renderChildren(stack, partialTicks);
            return;
        }

        currentItemStack.setCount(itemCount);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        renderItem(itemRenderer, screen.getFont(), currentItemStack, (int) absoluteX(), (int) absoluteY(), showCountText);

        renderChildren(stack, partialTicks);

        if (enableTooltip && !ItemUtils.isItemNull(currentItemStack)) {
            int mouseX = (int) screen.inputState().mouseX();
            int mouseY = (int) screen.inputState().mouseY();
            if (mouseInside) {
                renderTooltip(stack, mouseX, mouseY, currentItemStack);
            }
        }
    }

    /**
     * 获取当前的物品堆栈
     */
    @Nullable
    private ItemStack getCurrentItemStack() {
        if (itemStack != null) {
            return itemStack.copy();
        }

        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        return ItemUtils.deserializeItemStack(itemId);
    }

    /**
     * 渲染 Tooltip
     */
    private void renderTooltip(PoseStack stack, int mouseX, int mouseY, ItemStack itemStack) {
        List<Component> tooltip = itemStack.getTooltipLines(
                Minecraft.getInstance().player,
                InputStateManager.isShiftPressingStatic() ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );

        if (!tooltip.isEmpty()) {
            stack.pushPose();
            stack.translate(-absoluteX(), -absoluteY(), 0);
            if (vanillaTooltip) {
                screen.renderComponentTooltip(stack, tooltip, mouseX, mouseY);
            } else {
                EnumSeason season = seasonTooltip && screen != null ? screen.season() : null;
                TooltipWidget.drawItemTooltip(stack, itemStack, mouseX, mouseY, season);
            }
            stack.popPose();
        }
    }

    /**
     * 设置物品ID
     */
    public ItemWidget itemId(String itemId) {
        this.itemId = itemId;
        this.itemStack = null;
        return this;
    }

    /**
     * 设置物品堆栈
     */
    public ItemWidget itemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.itemId = null;
        return this;
    }


    /**
     * 用方块图集精灵做平面绘制，走 {@link com.mojang.blaze3d.vertex.PoseStack} 与 {@link AbstractGuiUtils#blit}
     */
    public static void renderGuiItemFlatBlit(@Nonnull PoseStack pose, @Nonnull Minecraft mc, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, null, mc.player, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        int tint = -1;
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

    /**
     * 在 GUI 中按像素边长绘制物品堆栈
     */
    public static void renderGuiItemScaled(@Nonnull Minecraft mc, @Nonnull ItemStack stack, int x, int y, int size) {
        if (size <= 0 || stack.isEmpty()) {
            return;
        }
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float s = size / 16f;
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            modelView.translate(x, y, 200f);
            modelView.scale(s, s, s);
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

    /**
     * 绘制物品图标
     */
    public static void renderItem(ItemRenderer itemRenderer, Font font, ItemStack itemStack, int x, int y, boolean showText) {
        renderGuiItemScaled(Minecraft.getInstance(), itemStack, x, y, 16);
        if (showText) {
            itemRenderer.renderGuiItemDecorations(font, itemStack, x, y, String.valueOf(itemStack.getCount()));
        }
    }
}
